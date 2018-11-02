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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.notification.dispatcher.Dispatcher;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * @since XXX
 */
public class NotificationProcessor implements StreamProcessorTopology {
    public static final String INPUT = "eventStream";

    public static final String OUTPUT = "notifcationStream";

    @Override
    public Topology getTopology(Map<String, String> options) {
        Topology.Builder builder = Topology.builder() //
                .addComputation(EventToNotificationComputation::new,
                        Arrays.asList("i1:" + INPUT, "o1:" + OUTPUT));

        Collection<Dispatcher> dispatchers = Framework.getService(NotificationService.class).getDispatchers();
        builder.addComputation(() -> new NotificationResolveDispatcher(dispatchers.size()),
                computeDispatchersIO(dispatchers));
        dispatchers.forEach(d -> builder.addComputation(() -> d, Collections.singletonList("i1:" + d.getName())));
        return builder.build();
    }

    protected List<String> computeDispatchersIO(Collection<Dispatcher> dispatchers) {
        List<String> res = new ArrayList<>();
        res.add("i1:" + OUTPUT);
        // XXX Ahem...
        dispatchers.forEach(d -> res.add("o" + res.size() + ":" + d.getName()));
        return res;
    }

    protected static class EventToNotificationComputation extends AbstractComputation {
        public EventToNotificationComputation() {
            super("eventToNotificationComputation", 1, 1);
        }

        @Override
        public void processRecord(ComputationContext ctx, String s, Record record) {
            ctx.produceRecord(OUTPUT, record);
            ctx.askForCheckpoint();
        }
    }

    protected static class NotificationResolveDispatcher extends AbstractComputation {

        public NotificationResolveDispatcher(int nbOutputStreams) {
            super("notificationResolveDispatcher", 1, nbOutputStreams);
        }

        @Override
        public void processRecord(ComputationContext ctx, String s, Record record) {
            metadata.outputStreams().forEach(os -> ctx.produceRecord(os, record));
            ctx.askForCheckpoint();
        }
    }
}
