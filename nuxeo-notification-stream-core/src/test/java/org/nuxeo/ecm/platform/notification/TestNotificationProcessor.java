/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.platform.notification.resolver.TestEventOnlyResolver.TARGET_USERS;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.time.Duration;
import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.notification.computation.EventToNotificationComputation;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.ecm.platform.notification.notifier.CounterNotifier;
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
        assertThat(topology.getAncestorComputationNames(EventToNotificationComputation.ID)).isEmpty();
        assertThat(topology.getDescendantComputationNames(EventToNotificationComputation.ID)).containsOnly("inApp",
                "log", "notEnabled");
    }

    @Test
    public void testTopologyExecution() throws InterruptedException {
        // Create a record in the stream in input of the notification processor
        LogManager logManager = nsc.getLogManager(nsc.getLogConfigNotification());
        assertThat(logManager.getAppender(nsc.getEventInputStream())).isNotNull();

        LogAppender<Record> appender = logManager.getAppender(nsc.getEventInputStream());
        EventRecord eventRecord = EventRecord.builder().withEventName("test").withUsername("Administrator").build();
        Record r = Record.of("toto", codecService.getCodec(DEFAULT_CODEC, EventRecord.class).encode(eventRecord));
        appender.append("toto", r);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        TestNotificationHelper.waitProcessorsCompletion(logManager, Duration.ofSeconds(5));
        // We have 2 notifiers enabled and 1 disabled all using the same class, and 1 resolver that has
        // TestEventOnlyResolver.TARGET_USERS users.
        // So, we expect to have nb_enabled_notifiers * nb_TARGET_USERS executions
        assertThat(CounterNotifier.processed).isEqualTo(TARGET_USERS * 2);
    }
}
