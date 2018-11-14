/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.computation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.NotificationStreamConfig;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test class for the computation EventToNotificationComputation.
 *
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/test-computations-contrib.xml")
public class TestEventToNotificationComputation {

    @Inject
    protected CodecService codecService;

    @Inject
    protected NotificationStreamConfig nsc;

    @Test
    public void testComputation() {
        EventToNotificationComputation comp = new EventToNotificationComputation();

        // Check expected metadata
        assertThat(comp.metadata().name()).isEqualTo(EventToNotificationComputation.NAME);
        assertThat(comp.metadata().inputStreams()).containsOnly("i1");
        assertThat(comp.metadata().outputStreams()).containsOnly("o1");

        // Create a context
        Map<String, String> mapping = new HashMap<>();
        mapping.put("i1", nsc.getEventInputStream());
        mapping.put("o1", nsc.getNotificationOutputStream());
        ComputationContextImpl context = new ComputationContextImpl(
                new ComputationMetadataMapping(comp.metadata(), mapping));

        // Ask to process a record that will not be processed by any contributed resolver
        EventRecord.EventRecordBuilder builder = EventRecord.builder();
        builder.withEventName("testEvent").withUsername("user1");
        Codec<EventRecord> codec = codecService.getCodec(DEFAULT_CODEC, EventRecord.class);
        comp.processRecord(context, "i1", Record.of("foo", codec.encode(builder.build())));

        // Check that the record has been processed and no Notification has been pushed to the output stream
        assertThat(context.getRecords(nsc.getNotificationOutputStream())).hasSize(0);

        // Ask to process a record that will not be processed by any contributed resolver
        builder.withEventName("documentCreated");
        comp.processRecord(context, "i1", Record.of("foo", codec.encode(builder.build())));

        // Check that the record has been processed and a Notification per user has been pushed to the output stream
        assertThat(context.getRecords(nsc.getNotificationOutputStream())).hasSize(3);
    }
}
