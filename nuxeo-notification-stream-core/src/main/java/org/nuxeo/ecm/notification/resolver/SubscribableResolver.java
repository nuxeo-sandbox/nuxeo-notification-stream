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
package org.nuxeo.ecm.notification.resolver;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.model.Subscribers;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * Subscribable Resolver that requires an explicit user's subscription to replace his target users (eg. When a user
 * wants to be notified for action on a specific document).
 * <p>
 * This abstract class stores subscriptions in a KVS based on an unique key per context.
 *
 * @since XXX
 */
public abstract class SubscribableResolver extends Resolver {

    public static final String KVS_SUBSCRIPTIONS = "notificationSubscriptions";

    public static final String DELIMITER = ":";

    /**
     * Compute storage key depending of a context. For instance, to make a difference between subscribers of different
     * events, or a docId
     *
     * @param ctx A map of String used to defined what store to use.
     */
    public String computeSubscriptionsKey(Map<String, String> ctx) {
        return id + DELIMITER
                + getRequiredContextFields().stream().map(ctx::get).collect(Collectors.joining(DELIMITER));
    }

    /**
     * List require fields in order to be able to subscribe to the Resolver
     * 
     * @return list of String, should not return null value.
     */
    public abstract List<String> getRequiredContextFields();

    protected boolean hasRequiredFields(Map<String, String> ctx) {
        return getRequiredContextFields().stream()
                                         .map(s -> ctx.getOrDefault(s, null))
                                         .allMatch(StringUtils::isNotEmpty);
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        return getSubscriptions(buildNotifierContext(eventRecord)).getUsernames();
    }

    /**
     * @param username
     * @param ctx
     */
    public void subscribe(String username, Map<String, String> ctx) {
        resolveSubscriptions(ctx, (s) -> {
            if (s == null) {
                return Subscribers.withUser(username);
            } else {
                return s.addUsername(username);
            }
        }, true);
    }

    /**
     * @param username
     * @param ctx
     */
    public void unsubscribe(String username, Map<String, String> ctx) {
        resolveSubscriptions(ctx, (s) -> s.remove(username), true);
    }

    protected Subscribers resolveSubscriptions(Map<String, String> ctx, Function<Subscribers, Subscribers> func,
            Boolean updateStorage) {
        if (!hasRequiredFields(ctx)) {
            List<String> retained = new ArrayList<>(getRequiredContextFields());
            retained.removeAll(ctx.keySet());
            throw new NuxeoException("Missing required context fields: " + StringUtils.join(retained, ", "));
        }

        KeyValueStore kvs = Framework.getService(KeyValueService.class).getKeyValueStore(KVS_SUBSCRIPTIONS);
        Codec<Subscribers> codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, Subscribers.class);
        String subscriptionsKey = computeSubscriptionsKey(ctx);
        byte[] bytes = kvs.get(subscriptionsKey);

        Subscribers subs = bytes == null ? null : codec.decode(bytes);
        if (func != null) {
            subs = func.apply(subs);
        }

        if (updateStorage) {
            kvs.put(subscriptionsKey, codec.encode(subs));
        }
        return subs;
    }

    public Subscribers getSubscriptions(Map<String, String> ctx) {
        return resolveSubscriptions(ctx, (s) -> s == null ? Subscribers.empty() : s, false);
    }
}
