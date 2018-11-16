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
package org.nuxeo.ecm.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.Collections;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.notification.computation.EventToNotificationComputation;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.notifier.CounterNotifier;
import org.nuxeo.ecm.notification.resolver.TestEventOnlyResolver;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/count-executions-contrib.xml")
public class TestNotificationProcessor {

    @Inject
    protected NotificationStreamConfig nsc;

    @Inject
    protected CodecService codecService;

    @Test
    public void testTopologyDefinition() {
        Topology topology = nsc.getTopology(Collections.emptyMap());
        assertThat(topology.streamsSet()).hasSize(4);
        assertThat(topology.getAncestorComputationNames(EventToNotificationComputation.NAME)).isEmpty();
        assertThat(topology.getDescendantComputationNames(EventToNotificationComputation.NAME)).containsOnly("inApp",
                "log", "notEnabled");
    }

    @Test
    public void testTopologyExecution() {
        // Create a record in the stream in input of the notification processor
        LogManager logManager = nsc.getLogManager(nsc.getLogConfigNotification());
        assertThat(logManager.getAppender(nsc.getEventInputStream())).isNotNull();

        LogAppender<Record> appender = logManager.getAppender(nsc.getEventInputStream());
        EventRecord eventRecord = EventRecord.builder().withEventName("test").withUsername("Administrator").build();
        Record r = Record.of("toto", codecService.getCodec(DEFAULT_CODEC, EventRecord.class).encode(eventRecord));
        appender.append("toto", r);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        TestNotificationHelper.waitProcessorsCompletion();
        // We have 2 notifiers enabled and 1 disabled all using the same class, and 1 resolver that has
        // TestEventOnlyResolver.TARGET_USERS users.
        // So, we expect to have nb_enabled_notifiers * nb_TARGET_USERS executions
        Assertions.assertThat(CounterNotifier.processed).isEqualTo(TestEventOnlyResolver.TARGET_USERS * 2);
    }
}
