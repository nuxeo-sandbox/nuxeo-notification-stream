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

import java.util.List;
import java.util.stream.IntStream;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * @since XXX
 */
public class EventToNotificationComputation extends AbstractComputation {
    public static final String ID = "eventToNotificationComputation";

    public static final String NOTIFICATION_USERS_BATCH_SIZE_PROP = "nuxeo.stream.notification.computation.user.batch.size";

    public static final String DEFAULT_USERS_BATCH_SIZE = "10";

    public EventToNotificationComputation() {
        super(EventToNotificationComputation.ID, 1, 1);
    }

    @Override
    public void processRecord(ComputationContext ctx, String s, Record record) {
        String outputStream = ((NotificationComponent) Framework.getService(
                NotificationService.class)).getNotificationOutputStream();

        Event event = new EventImpl("myEvent", null); // XXX FIXME
        Framework.getService(NotificationService.class).getResolvers(event).forEach(r -> {
            List<String> targetUsers = r.resolveTargetUsers(event);

            // Split target users in several batches in order to parallelize their preferences resolution
            int size = getBatchSize();
            IntStream.range(0, (targetUsers.size() + size - 1) / size)
                     .mapToObj(i -> targetUsers.subList(i * size, Math.min(targetUsers.size(), (i + 1) * size)))
                     .map(usersBatch -> Notification.builder().fromEvent(event).withUsernames(usersBatch).build())
                     .map(this::encodeNotif)
                     .forEach(notifRecord -> ctx.produceRecord(outputStream, notifRecord));
        });

        ctx.askForCheckpoint();
    }

    protected Record encodeNotif(Notification notif) {
        return Record.of(notif.id, Framework.getService(CodecService.class) //
                                            .getCodec(DEFAULT_CODEC, Notification.class)
                                            .encode(notif));
    }

    protected int getBatchSize() {
        return Integer.parseInt(Framework.getProperty(NOTIFICATION_USERS_BATCH_SIZE_PROP, DEFAULT_USERS_BATCH_SIZE));
    }
}
