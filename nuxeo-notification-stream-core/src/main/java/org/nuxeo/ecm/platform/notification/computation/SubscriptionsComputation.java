/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.computation;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import org.nuxeo.ecm.platform.notification.NotificationStreamCallback;
import org.nuxeo.ecm.platform.notification.message.SubscriptionAction;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * Computation for saving the new subscriptions or unsubscribe users for a given notification.
 *
 * @since XXX
 */
public class SubscriptionsComputation extends AbstractComputation {

    public static final String ID = "subscriptionsComputation";

    public SubscriptionsComputation() {
        super(ID, 1, 0);
    }

    @Override
    public void processRecord(ComputationContext computationContext, String inputStreamName, Record record) {
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
        computationContext.askForCheckpoint();
    }
}
