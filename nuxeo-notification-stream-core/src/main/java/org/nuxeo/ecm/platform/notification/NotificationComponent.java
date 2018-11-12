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

package org.nuxeo.ecm.platform.notification;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.notification.dispatcher.Dispatcher;
import org.nuxeo.ecm.platform.notification.dispatcher.DispatcherDescriptor;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.ecm.platform.notification.message.SubscriptionActionRecord;
import org.nuxeo.ecm.platform.notification.message.UserResolverSettings;
import org.nuxeo.ecm.platform.notification.processors.EventToNotificationComputation;
import org.nuxeo.ecm.platform.notification.resolver.Resolver;
import org.nuxeo.ecm.platform.notification.resolver.ResolverDescriptor;
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
    public static final String XP_DISPATCHER = "dispatcher";

    public static final String XP_RESOLVER = "resolver";

    public static final String XP_SETTINGS = "settings";

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

    public static final String LOG_CONFIG_SETTINGS_PROP = "nuxeo.stream.notification.settings.log.config";

    public static final String DEFAULT_LOG_CONFIG_SETTINGS = "notificationSettings";

    public static final String KVS_SETTINGS = "notificationSettings";

    public static final String KVS_SUBSCRIPTIONS = "notificationSubscriptions";

    protected Map<String, Dispatcher> dispatchers = new ConcurrentHashMap<>();

    protected Map<String, Resolver> resolvers = new ConcurrentHashMap<>();

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
        if (XP_DISPATCHER.equals(xp)) {
            dispatchers.remove(desc.getId());
        } else if (XP_RESOLVER.equals(xp)) {
            resolvers.remove(desc.getId());
        }
    }

    protected void createContributionInstance(Object contribution, String xp) {
        if (!(contribution instanceof Descriptor)) {
            return;
        }

        Descriptor desc = (Descriptor) contribution;
        if (XP_DISPATCHER.equals(xp)) {
            dispatchers.put(desc.getId(), ((DispatcherDescriptor) desc).newInstance());
        } else if (XP_RESOLVER.equals(xp)) {
            resolvers.put(desc.getId(), ((ResolverDescriptor) desc).newInstance());
        }
    }

    @Override
    public Topology buildTopology(Map<String, String> options) {
        Topology.Builder builder = Topology.builder().addComputation(EventToNotificationComputation::new,
                Arrays.asList("i1:" + getEventInputStream(), "o1:" + getNotificationOutputStream()));

        Collection<Dispatcher> dispatchers = Framework.getService(NotificationService.class).getDispatchers();
        dispatchers.forEach(
                d -> builder.addComputation(() -> d, Collections.singletonList("i1:" + getNotificationOutputStream())));
        return builder.build();
    }

    @Override
    public void subscribe(String username, String resolverId, Map<String, String> ctx) {
        appendSubscriptionActionRecord(SubscriptionActionRecord.subscribe(username, resolverId, ctx));
    }

    @Override
    public void unsubscribe(String username, String resolverId, Map<String, String> ctx) {
        appendSubscriptionActionRecord(SubscriptionActionRecord.unsubscribe(username, resolverId, ctx));
    }

    public void appendSubscriptionActionRecord(SubscriptionActionRecord record) {
        LogAppender<Record> appender = getLogManager(getLogConfigSubscriptions()).getAppender(getNotificationSubscriptionsStream());
        appender.append(record.getId(), Record.of(record.getId(), record.encode()));
    }

    @Override
    public void doSubscribe(String username, String resolverId, Map<String, String> ctx) {
        resolveSubscriptions(resolverId, ctx, (s) -> {
            if (s == null) {
                return Subscriptions.withUser(username);
            } else {
                return s.addUsername(username);
            }
        }, true);
    }

    @Override
    public void doUnsubscribe(String username, String resolverId, Map<String, String> ctx) {
        resolveSubscriptions(resolverId, ctx, (s) -> s.remove(username), true);
    }

    @Override
    public Subscriptions getSubscriptions(String resolverId, Map<String, String> ctx) {
        return resolveSubscriptions(resolverId, ctx, (s) -> {
            return s == null ? Subscriptions.empty() : s;
        }, false);
    }

    protected Subscriptions resolveSubscriptions(String resolverId, Map<String, String> ctx,
            Function<Subscriptions, Subscriptions> func, Boolean updateStorage) {
        Resolver resolver = getResolver(resolverId);
        if (resolver == null) {
            throw new NuxeoException("Unknown resolver with id " + resolverId);
        }

        KeyValueStore kvs = getKeyValueStore(KVS_SUBSCRIPTIONS);
        Codec<Subscriptions> codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC,
                Subscriptions.class);
        String subscriptionsKey = resolver.computeSubscriptionsKey(ctx);
        byte[] bytes = kvs.get(subscriptionsKey);

        Subscriptions subs = bytes == null ? null : codec.decode(bytes);
        if (func != null) {
            subs = func.apply(subs);
        }

        if (updateStorage) {
            kvs.put(subscriptionsKey, codec.encode(subs));
        }
        return subs;
    }

    @Override
    public Dispatcher getDispatcher(String id) {
        return dispatchers.get(id);
    }

    @Override
    public Collection<Dispatcher> getDispatchers() {
        return dispatchers.values();
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
    public String getLogConfigSettings() {
        return Framework.getProperty(LOG_CONFIG_SETTINGS_PROP, DEFAULT_LOG_CONFIG_SETTINGS);
    }

    @Override
    public String getLogConfigSubscriptions() {
        return Framework.getProperty(LOG_CONFIG_PROP, DEFAULT_LOG_CONFIG);
    }

    @Override
    public void updateSettings(String username, String resolverId, Map<String, Boolean> dispatchersSettings) {
        // Get the resolver
        Resolver resolver = resolvers.get(resolverId);
        if (resolver == null) {
            throw new NuxeoException("The resolver " + resolverId + " does not exist.");
        }

        // Update the settings
        UserResolverSettings newSettings = new UserResolverSettings();
        newSettings.setSettings(dispatchersSettings);
        KeyValueStore settingsKVS = getKeyValueStore(KVS_SETTINGS);
        Codec<UserResolverSettings> avroCodec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC,
                UserResolverSettings.class);
        settingsKVS.put(username + ":" + resolverId, avroCodec.encode(newSettings));
    }

    @Override
    public boolean hasSpecificSettings(String username, String resolverId) {
        return false;
    }

    @Override
    public List<Dispatcher> getDispatchers(String username, String resolverId) {
        // Get the settings for the given user and the given resolver
        KeyValueStore store = getKeyValueStore(KVS_SETTINGS);
        byte[] userSettingsBytes = store.get(username + ":" + resolverId);

        List<String> defaults = getDefaults(resolverId);
        // If there is not settings defined for the user and the resolver, the default settings are returned
        if (userSettingsBytes == null) {
            return getDispatchers(defaults);
        }

        Codec<UserResolverSettings> avroCodec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC,
                UserResolverSettings.class);
        UserResolverSettings userResolverSettings = avroCodec.decode(userSettingsBytes);

        // Add missing settings for default enabled dispatchers
        defaults.stream() //
                .filter(d -> !userResolverSettings.getSettings().containsKey(d))
                .forEach(d -> userResolverSettings.getSettings().put(d, true));

        return getDispatchers(userResolverSettings.getEnabledDispatchers());
    }

    protected List<Dispatcher> getDispatchers(List<String> dispatchers) {
        if (dispatchers == null || dispatchers.isEmpty()) {
            return Collections.emptyList();
        }

        return getDispatchers().stream() //
                               .filter(d -> dispatchers.contains(d.getName()))
                               .collect(Collectors.toList());
    }

    protected List<String> getDefaults(String resolverId) {
        SettingsDescriptor descriptor = getSetting(resolverId);
        if (descriptor == null) {
            // The descriptor for the resolver does not exist, null is returned
            return null;
        }

        Map<String, SettingsDescriptor.DispatcherSetting> settings = descriptor.getSettings();

        // Filter the dispatcher to remove all the disabled and not active by default
        return settings.entrySet().stream().filter(s -> {
            SettingsDescriptor.DispatcherSetting setting = s.getValue();
            return setting.isEnabled() && setting.isDefault();
        }).map(map -> map.getKey()).collect(Collectors.toList());
    }

    protected SettingsDescriptor getSetting(String resolverId) {
        return (SettingsDescriptor) getDescriptor(XP_SETTINGS, resolverId);
    }

    protected KeyValueStore getKeyValueStore(String storeId) {
        return Framework.getService(KeyValueService.class).getKeyValueStore(storeId);
    }
}
