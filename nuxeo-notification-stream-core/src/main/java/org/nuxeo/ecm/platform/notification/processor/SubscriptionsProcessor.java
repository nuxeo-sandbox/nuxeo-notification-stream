/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.processor;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.platform.notification.NotificationStreamCallback;
import org.nuxeo.ecm.platform.notification.NotificationStreamConfig;
import org.nuxeo.ecm.platform.notification.message.SubscriptionAction;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * StreamProcessor for saving the new subscriptions or unsubscribe users for a given notification.
 *
 * @since XXX
 */
public class SubscriptionsProcessor implements StreamProcessorTopology {

    @Override
    public Topology getTopology(Map<String, String> map) {
        String notificationSubscriptionsStream = Framework.getService(NotificationStreamConfig.class)
                                                          .getNotificationSubscriptionsStream();
        return Topology.builder()
                       .addComputation(SubscriptionsComputation::new,
                               Collections.singletonList("i1:" + notificationSubscriptionsStream))
                       .build();
    }

    protected class SubscriptionsComputation extends AbstractComputation {

        public static final String ID = "subscriptionsComputation";

        public SubscriptionsComputation() {
            super(ID, 1, 0);
        }

        @Override
        public void processRecord(ComputationContext computationContext, String key, Record record) {
            Codec<SubscriptionAction> codec = Framework.getService(CodecService.class)
                                                             .getCodec(DEFAULT_CODEC, SubscriptionAction.class);
            SubscriptionAction sub = codec.decode(record.getData());
            NotificationStreamCallback scb = Framework.getService(NotificationStreamCallback.class);

            switch (sub.getAction()) {
            case SUBSCRIBE:
                scb.doSubscribe(sub.getUsername(), sub.getResolverId(), sub.getCtx());
                break;
            case UNSUBSCRIBE:
                scb.doUnsubscribe(sub.getUsername(), sub.getResolverId(), sub.getCtx());
                break;
            }
            // End the computation
            computationContext.askForCheckpoint();
        }
    }
}
