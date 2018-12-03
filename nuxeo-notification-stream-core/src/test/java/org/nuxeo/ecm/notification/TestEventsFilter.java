/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.notification.TestNotificationHelper.readRecord;
import static org.nuxeo.ecm.notification.transformer.EventFilteringTransformer.PROP_FLAG_FILTER;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test the filtering of EventRecords before appending them to the input stream.
 *
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/test-events-filtering-contrib.xml")
public class TestEventsFilter {

    @Inject
    protected EventService eventService;

    @Inject
    protected NotificationStreamConfig nsc;

    @Inject
    protected CodecService codecService;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void testFilteredEvents() throws InterruptedException {
        // Fire several events, a few with the flag so they're filtered and other without it
        EventContext eventContextNoFilter = new EventContextImpl();
        eventContextNoFilter.setCoreSession(session);
        eventContextNoFilter.setPrincipal(session.getPrincipal());
        eventContextNoFilter.getProperties().put(PROP_FLAG_FILTER, false);
        EventContext eventContextFilter = new EventContextImpl();
        eventContextFilter.setCoreSession(session);
        eventContextFilter.setPrincipal(session.getPrincipal());
        eventContextFilter.getProperties().put(PROP_FLAG_FILTER, true);

        // Send the events in the same transaction
        txFeature.nextTransaction();
        eventService.fireEvent("event1", eventContextNoFilter);
        eventService.fireEvent("event2", eventContextFilter);
        eventService.fireEvent("event3", eventContextFilter);
        eventService.fireEvent("event4", eventContextNoFilter);
        txFeature.nextTransaction();
        TestNotificationHelper.waitProcessorsCompletion();

        // Check the EventRecords produced
        Record recordEvent1 = readRecord(nsc.getEventInputStream(), nsc.getEventInputStream());
        Record recordEvent2 = readRecord(nsc.getEventInputStream(), nsc.getEventInputStream());
        assertThat(recordEvent1).isNotNull();
        assertThat(recordEvent2).isNotNull();

        Codec<EventRecord> codec = codecService.getCodec(DEFAULT_CODEC, EventRecord.class);
        EventRecord eventRecord1 = codec.decode(recordEvent1.getData());
        EventRecord eventRecord2 = codec.decode(recordEvent2.getData());
        assertThat(eventRecord1.getEventName()).isIn("event1", "event4");
        assertThat(eventRecord2.getEventName()).isIn("event1", "event4");
    }

}
