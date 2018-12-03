/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Nuxeo
 */

package org.nuxeo.ecm.notification;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.notification.computation.EventToNotificationComputation;
import org.nuxeo.ecm.notification.computation.SaveNotificationSettingsComputation;
import org.nuxeo.ecm.notification.computation.SubscriptionsComputation;
import org.nuxeo.ecm.notification.event.EventFilter;
import org.nuxeo.ecm.notification.event.EventFilterDescriptor;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.message.SubscriptionAction;
import org.nuxeo.ecm.notification.message.UserSettings;
import org.nuxeo.ecm.notification.model.Subscribers;
import org.nuxeo.ecm.notification.model.UserNotifierSettings;
import org.nuxeo.ecm.notification.notifier.Notifier;
import org.nuxeo.ecm.notification.notifier.NotifierDescriptor;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.notification.resolver.ResolverDescriptor;
import org.nuxeo.ecm.notification.resolver.SubscribableResolver;
import org.nuxeo.ecm.notification.transformer.EventTransformer;
import org.nuxeo.ecm.notification.transformer.EventTransformerDescriptor;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.kafka.KafkaConfigServiceImpl;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.stream.StreamService;

/**
 * @since XXX
 */
public class NotificationComponent extends DefaultComponent implements NotificationService, NotificationSettingsService,
        NotificationStreamConfig, NotificationStreamCallback {
    public static final String XP_NOTIFIER = "notifier";

    public static final String XP_RESOLVER = "resolver";

    public static final String XP_SETTINGS = "settings";

    public static final String XP_EVENT_TRANSFORMERS = "eventTransformer";

    public static final String XP_EVENT_FILTER = "filter";

    public static final String STREAM_OUPUT_PROP = "nuxeo.stream.notification.output";

    public static final String DEFAULT_STREAM_OUTPUT = "notificationOutput";

    public static final String STREAM_SUBSCRIPTIONS_PROP = "nuxeo.stream.notification.subscriptions";

    public static final String DEFAULT_STREAM_SUBSCRIPTIONS = "subscriptions";

    public static final String STREAM_INPUT_PROP = "nuxeo.stream.notification.input";

    public static final String DEFAULT_STREAM_INPUT = "notificationInput";

    public static final String LOG_CONFIG_PROP = "nuxeo.stream.notification.log.config";

    public static final String DEFAULT_LOG_CONFIG = "notification";

    public static final String STREAM_SETTINGS_PROP = "nuxeo.stream.notification.settings.input";

    public static final String DEFAULT_STREAM_SETTINGS = "notificationSettings";

    public static final String KVS_SETTINGS = "notificationSettings";

    protected Map<String, Notifier> notifiers = new ConcurrentHashMap<>();

    protected Map<String, Resolver> resolvers = new ConcurrentHashMap<>();

    protected Map<String, EventTransformer> eventTransformers = new ConcurrentHashMap<>();

    protected Map<String, EventFilter> eventFilters = new ConcurrentHashMap<>();

    @Override
    public int getApplicationStartedOrder() {
        // Topology is dependent of contributions, and we need to be sure this component is started before the
        // StreamService component.
        return KafkaConfigServiceImpl.APPLICATION_STARTED_ORDER + 5;
    }

    @Override
    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        super.registerContribution(contribution, xp, component);
        createContributionInstance(contribution, xp);
    }

    @Override
    public void unregisterContribution(Object contribution, String xp, ComponentInstance component) {
        super.unregisterContribution(contribution, xp, component);
        removeContributionInstance(contribution, xp);
    }

    protected void removeContributionInstance(Object contribution, String xp) {
        if (!(contribution instanceof Descriptor)) {
            return;
        }

        Descriptor desc = (Descriptor) contribution;
        if (XP_NOTIFIER.equals(xp)) {
            notifiers.remove(desc.getId());
        } else if (XP_RESOLVER.equals(xp)) {
            resolvers.remove(desc.getId());
        } else if (XP_EVENT_TRANSFORMERS.equals(xp)) {
            eventTransformers.remove(desc.getId());
        } else if (XP_EVENT_FILTER.equals(xp)) {
            eventFilters.remove(desc.getId());
        }
    }

    protected void createContributionInstance(Object contribution, String xp) {
        if (!(contribution instanceof Descriptor)) {
            return;
        }

        Descriptor desc = (Descriptor) contribution;
        if (XP_NOTIFIER.equals(xp)) {
            notifiers.put(desc.getId(), ((NotifierDescriptor) desc).newInstance());
        } else if (XP_RESOLVER.equals(xp)) {
            resolvers.put(desc.getId(), ((ResolverDescriptor) desc).newInstance());
        } else if (XP_EVENT_TRANSFORMERS.equals(xp)) {
            eventTransformers.put(desc.getId(), ((EventTransformerDescriptor) desc).newInstance());
        } else if (XP_EVENT_FILTER.equals(xp)) {
            eventFilters.put(desc.getId(), ((EventFilterDescriptor) desc).newInstance());
        }
    }

    @Override
    public Topology getTopology(Map<String, String> options) {
        Topology.Builder builder = Topology.builder()
                                           .addComputation(EventToNotificationComputation::new,
                                                   Arrays.asList("i1:" + getEventInputStream(),
                                                           "o1:" + getNotificationOutputStream()));

        Collection<Notifier> notifiers = Framework.getService(NotificationService.class).getNotifiers();
        notifiers.forEach(
                d -> builder.addComputation(() -> d, Collections.singletonList("i1:" + getNotificationOutputStream())));
        // Add the computation for the Notification Settings
        builder.addComputation(SaveNotificationSettingsComputation::new,
                Collections.singletonList("i1:" + getNotificationSettingsInputStream()));
        // Add the computation for the subscriptions
        builder.addComputation(SubscriptionsComputation::new,
                Collections.singletonList("i1:" + getNotificationSubscriptionsStream()));
        return builder.build();
    }

    @Override
    public void subscribe(String username, String resolverId, Map<String, String> ctx) {
        appendSubscriptionActionRecord(SubscriptionAction.subscribe(username, resolverId, ctx));
    }

    @Override
    public void unsubscribe(String username, String resolverId, Map<String, String> ctx) {
        appendSubscriptionActionRecord(SubscriptionAction.unsubscribe(username, resolverId, ctx));
    }

    public void appendSubscriptionActionRecord(SubscriptionAction record) {
        LogAppender<Record> appender = getLogManager(getLogConfigNotification()).getAppender(
                getNotificationSubscriptionsStream());
        appender.append(record.getId(), Record.of(record.getId(), record.encode()));
    }

    @Override
    public void doSubscribe(String username, String resolverId, Map<String, String> ctx) {
        getSubscribableResolver(resolverId).subscribe(username, ctx);
    }

    @Override
    public void doUnsubscribe(String username, String resolverId, Map<String, String> ctx) {
        getSubscribableResolver(resolverId).unsubscribe(username, ctx);
    }

    @Override
    public boolean hasSubscribe(String username, String resolverId, Map<String, String> ctx) {
        return getSubscriptions(resolverId, ctx).getUsernames().anyMatch(s -> s.equals(username));
    }

    @Override
    public Subscribers getSubscriptions(String resolverId, Map<String, String> ctx) {
        return getSubscribableResolver(resolverId).getSubscriptions(ctx);
    }

    protected SubscribableResolver getSubscribableResolver(String resolverId) {
        Resolver resolver = getResolver(resolverId);
        if (resolver == null) {
            throw new NuxeoException("Unknown resolver with id " + resolverId);
        }

        if (!(resolver instanceof SubscribableResolver)) {
            throw new NuxeoException("You cannot subscribe to an unsubscribable resolver");
        }

        return (SubscribableResolver) resolver;
    }

    @Override
    public EventTransformer getEventTransformer(String id) {
        return eventTransformers.get(id);
    }

    @Override
    public Collection<EventTransformer> getEventTransformers() {
        return eventTransformers.values();
    }

    @Override
    public Collection<EventFilter> getEventFilters() {
        return eventFilters.values();
    }

    @Override
    public Notifier getNotifier(String id) {
        return notifiers.get(id);
    }

    @Override
    public Collection<Notifier> getNotifiers() {
        return notifiers.values();
    }

    @Override
    public Resolver getResolver(String id) {
        return resolvers.get(id);
    }

    @Override
    public Collection<Resolver> getResolvers() {
        return resolvers.values();
    }

    @Override
    public Collection<Resolver> getResolvers(EventRecord eventRecord) {
        return getResolvers().stream().filter(r -> r.accept(eventRecord)).collect(Collectors.toList());
    }

    @Override
    public String getEventInputStream() {
        return Framework.getProperty(STREAM_INPUT_PROP, DEFAULT_STREAM_INPUT);
    }

    @Override
    public String getNotificationOutputStream() {
        return Framework.getProperty(STREAM_OUPUT_PROP, DEFAULT_STREAM_OUTPUT);
    }

    @Override
    public String getNotificationSubscriptionsStream() {
        return Framework.getProperty(STREAM_SUBSCRIPTIONS_PROP, DEFAULT_STREAM_SUBSCRIPTIONS);
    }

    @Override
    public String getNotificationSettingsInputStream() {
        return Framework.getProperty(STREAM_SETTINGS_PROP, DEFAULT_STREAM_SETTINGS);
    }

    @Override
    public LogManager getLogManager(String logConfigName) {
        return Framework.getService(StreamService.class).getLogManager(logConfigName);
    }

    @Override
    public String getLogConfigNotification() {
        return Framework.getProperty(LOG_CONFIG_PROP, DEFAULT_LOG_CONFIG);
    }

    @Override
    public void updateSettings(String username, Map<String, UserNotifierSettings> userSettings) {
        LogAppender<Record> appender = getLogManager(getLogConfigNotification()).getAppender(
                getNotificationSettingsInputStream());
        UserSettings us = UserSettings.builder().withUsername(username).withSettings(userSettings).build();
        appender.append(us.getId(), Record.of(us.getId(), us.encode()));
    }

    @Override
    public UserSettings getResolverSettings(String username) {
        UserSettings.UserSettingsBuilder builder = UserSettings.builder();

        getResolvers().forEach(r -> {
            UserNotifierSettings urs = getUserResolverSettings(username, r.getId());
            builder.withSetting(r.getId(), urs);
        });

        return builder.build();
    }

    @Override
    public void doUpdateSettings(String username, String resolverId, Map<String, Boolean> notifiersSettings) {
        // Get the resolver
        Resolver resolver = resolvers.get(resolverId);
        if (resolver == null) {
            throw new NuxeoException("The resolver " + resolverId + " does not exist.");
        }

        // Update the settings
        // XXX Validate notifierSettings input
        UserNotifierSettings newSettings = UserNotifierSettings.builder().putSettings(notifiersSettings).build();

        KeyValueStore settingsKVS = getKeyValueStore(KVS_SETTINGS);
        Codec<UserNotifierSettings> avroCodec = Framework.getService(CodecService.class)
                                                         .getCodec(DEFAULT_CODEC, UserNotifierSettings.class);
        settingsKVS.put(username + ":" + resolverId, avroCodec.encode(newSettings));
    }

    @Override
    public boolean hasSpecificSettings(String username, String resolverId) {
        return false;
    }

    @Override
    public List<Notifier> getSelectedNotifiers(String username, String resolverId) {
        return getNotifiers(getUserResolverSettings(username, resolverId).getSelectedNotifiers());
    }

    protected UserNotifierSettings getUserResolverSettings(String username, String resolverId) {
        // Get the settings for the given user and the given resolver
        KeyValueStore store = getKeyValueStore(KVS_SETTINGS);
        byte[] userSettingsBytes = store.get(username + ":" + resolverId);

        UserNotifierSettings defaults = getDefaults(resolverId);
        // If there is not settings defined for the user and the resolver, the default settings are returned
        if (userSettingsBytes == null) {
            return defaults;
        }

        Codec<UserNotifierSettings> avroCodec = Framework.getService(CodecService.class)
                                                         .getCodec(DEFAULT_CODEC, UserNotifierSettings.class);
        UserNotifierSettings userNotifierSettings = avroCodec.decode(userSettingsBytes);

        return UserNotifierSettings.builder()
                                   .putSettings(defaults.getSettings())
                                   .putSettings(userNotifierSettings.getSettings())
                                   .build();
    }

    protected List<Notifier> getNotifiers(List<String> notifiers) {
        if (notifiers == null || notifiers.isEmpty()) {
            return Collections.emptyList();
        }

        return getNotifiers().stream() //
                             .filter(d -> notifiers.contains(d.getName()))
                             .collect(Collectors.toList());
    }

    protected UserNotifierSettings getDefaults(String resolverId) {
        SettingsDescriptor descriptor = getSetting(resolverId);
        if (descriptor == null) {
            boolean exists = resolvers.containsKey(resolverId);
            // The descriptor for the resolver does not exist, null is returned
            return exists ? UserNotifierSettings.defaultFromNotifiers(getNotifiers()) : null;
        }

        return UserNotifierSettings.defaultFromDescriptor(descriptor);
    }

    protected SettingsDescriptor getSetting(String resolverId) {
        return (SettingsDescriptor) getDescriptor(XP_SETTINGS, resolverId);
    }

    protected KeyValueStore getKeyValueStore(String storeId) {
        return Framework.getService(KeyValueService.class).getKeyValueStore(storeId);
    }
}
