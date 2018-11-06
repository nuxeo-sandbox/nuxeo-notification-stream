/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import javax.inject.Inject;
import java.time.Duration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.notification.NotificationFeature;
import org.nuxeo.ecm.platform.notification.NotificationService;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test class for the listener {@link EventsStreamListener}.
 *
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
public class TestEventsStreamListener {

    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationService service;

    @Inject
    protected EventService eventService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void listenerPushesEventToStream() throws Exception {
        // Create a new document
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc = session.createDocument(doc);
        session.save();

        // Wait for the end of the async listeners
        txFeature.nextTransaction();
        eventService.waitForAsyncCompletion();

        // Check the record in the stream
        LogManager logManager = service.getLogManager();
        Codec codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, Record.class);
        try (LogTailer<Record> tailer = logManager.createTailer(service.getEventInputStream(), service.getEventInputStream(), codec)) {
            LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(5));
            assertThat(logRecord).isNotNull();

            // Check the Record
            Record record = logRecord.message();
            assertThat(record.getKey()).isEqualTo(DOCUMENT_CREATED);

            // Check the EventRecord in the Record
            byte[] decodedEvent = record.getData();
            Codec codecMessage = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, EventRecord.class);
            EventRecord eventRecord = (EventRecord) codecMessage.decode(decodedEvent);
            assertThat(eventRecord.getEventName()).isEqualTo(DOCUMENT_CREATED);
            assertThat(eventRecord.getDocId()).isEqualTo(doc.getId());
            assertThat(eventRecord.getUsername()).isEqualTo("Administrator");
        }
        // never close the manager this is done by the service
    }
}
