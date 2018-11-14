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

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.model.Subscribers;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;

/**
 * @since XXX
 */
public abstract class SubscribableResolver extends Resolver {

    public static final String KVS_SUBSCRIPTIONS = "notificationSubscriptions";

    /**
     * Compute Storage key context based on the given EventRecord
     *
     * @param eventRecord contains informations needed to compute the key, in case we want to store subscriptions
     *            dependent of a context.
     * @return Context map, or an emptyMap in case the method hasn't been override.
     */
    protected abstract Map<String, String> computeContextFromEvent(EventRecord eventRecord);

    /**
     * Compute storage key depending of a context. For instance, to make a difference between subscribers of different
     * events, or a docId
     *
     * @param ctx A map of String used to defined what store to use.
     */
    public abstract String computeSubscriptionsKey(Map<String, String> ctx);

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        Subscribers subscribtions = getSubscriptions(computeContextFromEvent(eventRecord));
        return subscribtions == null ? Stream.empty() : subscribtions.getUsernames();
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
