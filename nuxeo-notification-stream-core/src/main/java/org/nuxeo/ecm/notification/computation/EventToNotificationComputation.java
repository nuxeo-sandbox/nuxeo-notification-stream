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

import org.apache.commons.lang3.tuple.Pair;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.NotificationStreamConfig;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * @since XXX
 */
public class EventToNotificationComputation extends AbstractComputation {

    public static final String NAME = "eventToNotificationComputation";

    public EventToNotificationComputation() {
        super(EventToNotificationComputation.NAME, 1, 1);
    }

    @Override
    public void processRecord(ComputationContext ctx, String inputStreamName, Record record) {
        String outputStream = Framework.getService(NotificationStreamConfig.class).getNotificationOutputStream();

        // Extract the EventRecord from the input Record
        EventRecord eventRecord = Framework.getService(CodecService.class) //
                                           .getCodec(DEFAULT_CODEC, EventRecord.class)
                                           .decode(record.getData());
        Framework.getService(NotificationService.class)
                 .getResolvers(eventRecord)
                 .stream()
                 .map(resolver -> Pair.of(resolver,
                         Notification.builder()
                                     .fromEvent(eventRecord)
                                     .withCtx(resolver.buildNotifierContext(eventRecord))
                                     .withResolver(resolver)))
                 .forEach(p -> p.getLeft()
                                .resolveTargetUsers(eventRecord)
                                .filter(user -> !user.equals(eventRecord.getUsername()))
                                .map(u -> p.getRight().withUsername(u).build())
                                .map(this::encodeNotif)
                                .forEach(notif -> ctx.produceRecord(outputStream, notif)));

        ctx.askForCheckpoint();
    }

    protected Record encodeNotif(Notification notif) {
        return Record.of(notif.getId(), Framework.getService(CodecService.class) //
                                                 .getCodec(DEFAULT_CODEC, Notification.class)
                                                 .encode(notif));
    }
}
