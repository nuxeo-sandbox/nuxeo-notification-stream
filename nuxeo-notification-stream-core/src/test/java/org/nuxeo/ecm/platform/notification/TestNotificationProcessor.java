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

import javax.inject.Inject;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.notification.dispatcher.LogDispatcher;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/dummy-contrib.xml")
public class TestNotificationProcessor {

    @Inject
    protected StreamService streamService;

    @Test
    public void testTopologyDefinition() {
        Topology topology = new NotificationProcessor().getTopology(Collections.emptyMap());
        assertThat(topology.streamsSet()).hasSize(4);
        assertThat(topology.getAncestorComputationNames("eventToNotificationComputation")).isEmpty();
        assertThat(topology.getAncestorComputationNames("notificationResolveDispatcher")).containsOnly("eventToNotificationComputation");
        assertThat(topology.getDescendantComputationNames("notificationResolveDispatcher")).containsOnly("inApp", "log");
    }

    @Test
    public void testTopologyExecution() throws InterruptedException {
        // Create a record in the stream in input of the notification processor
        LogManager logManager = streamService.getLogManager("default");
        assertThat(logManager.getAppender("eventStream")).isNotNull();
        LogAppender<Record> appender = logManager.getAppender("eventStream");
        appender.append("toto", Record.of("toto", "Test".getBytes()));

        TestNotificationHelper.awaitCompletion(logManager, 5, TimeUnit.SECONDS);
        assertThat(LogDispatcher.processed).isEqualTo(2);
    }

}
