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
package org.nuxeo.ecm.notification.computation;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import org.nuxeo.ecm.notification.NotificationStreamCallback;
import org.nuxeo.ecm.notification.message.SubscriptionAction;
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
