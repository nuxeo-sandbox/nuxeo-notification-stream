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
import org.nuxeo.ecm.platform.notification.resolver.AcceptAllResolver;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/empty-resolver-contrib.xml")
public class TestNotificationProcessor {

    @Inject
    protected NotificationService notificationService;

    @Test
    public void testTopologyDefinition() {
        Topology topology = new NotificationProcessor().getTopology(Collections.emptyMap());
        assertThat(topology.streamsSet()).hasSize(2);
        assertThat(topology.getAncestorComputationNames(EventToNotificationComputation.ID)).isEmpty();
        assertThat(topology.getDescendantComputationNames(EventToNotificationComputation.ID)).containsOnly("inApp",
                "log");
    }

    @Test
    public void testTopologyExecution() throws InterruptedException {
        // Create a record in the stream in input of the notification processor
        LogManager logManager = notificationService.getLogManager();
        assertThat(logManager.getAppender(notificationService.getEventInputStream())).isNotNull();

        LogAppender<Record> appender = logManager.getAppender(notificationService.getEventInputStream());
        Record r = Record.of("toto", "".getBytes());
        appender.append("toto", r);

        TestNotificationHelper.awaitCompletion(logManager, 5, TimeUnit.SECONDS);
        // AcceptAllResolver is multipliing by X target users batch size, to ensure the list is correctly splitted.
        assertThat(LogDispatcher.processed).isEqualTo(AcceptAllResolver.MULTIPLIER * 2);
    }

}
