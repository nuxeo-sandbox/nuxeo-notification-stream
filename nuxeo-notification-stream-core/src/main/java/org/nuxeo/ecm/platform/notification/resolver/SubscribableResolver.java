/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.resolver;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.nuxeo.ecm.platform.notification.NotificationService;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.ecm.platform.notification.model.Subscribers;
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
