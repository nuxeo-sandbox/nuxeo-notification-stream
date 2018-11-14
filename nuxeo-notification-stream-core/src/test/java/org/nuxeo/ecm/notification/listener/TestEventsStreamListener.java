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
package org.nuxeo.ecm.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.NotificationStreamConfig;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.test.runner.Deploy;
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
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/test-event-listener-contrib.xml")
public class TestEventsStreamListener {

    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationStreamConfig streamConfig;

    @Inject
    protected EventService eventService;

    @Inject
    protected TransactionalFeature txFeature;

    @Test
    public void listenerPushesEventToStream() throws InterruptedException {
        // Create a new document
        DocumentModel doc = session.createDocumentModel("/", "testDoc", "File");
        doc = session.createDocument(doc);
        session.save();

        // Wait for the end of the async listeners
        txFeature.nextTransaction();
        eventService.waitForAsyncCompletion();

        // Check the Record in the stream
        Record record = readRecord();
        assertThat(record.getKey()).isEqualTo("documentCreated:" + doc.getId());

        // Check the EventRecord in the Record
        byte[] decodedEvent = record.getData();
        Codec codecMessage = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, EventRecord.class);
        EventRecord eventRecord = (EventRecord) codecMessage.decode(decodedEvent);
        assertThat(eventRecord.getEventName()).isEqualTo(DOCUMENT_CREATED);
        assertThat(eventRecord.getDocumentSourceId()).isEqualTo(doc.getId());
        assertThat(eventRecord.getUsername()).isEqualTo("Administrator");

        // Trigger a non document event
        EventContext ctx = new EventContextImpl(session, session.getPrincipal());
        eventService.fireEvent("randomEvent", ctx);
        // Wait for the end of the async listeners
        txFeature.nextTransaction();
        eventService.waitForAsyncCompletion();

        // Check the Record in the stream
        record = readRecord();
        assertThat(record.getKey()).isEqualTo("randomEvent:null:Administrator");
    }

    protected Record readRecord() throws InterruptedException {
        // Check the record in the stream
        Codec<Record> codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, Record.class);
        LogManager logManager = streamConfig.getLogManager(streamConfig.getLogConfigNotification());
        try (LogTailer<Record> tailer = logManager.createTailer(streamConfig.getEventInputStream(),
                streamConfig.getEventInputStream(), codec)) {
            LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(5));
            tailer.commit();
            return logRecord.message();
        }
        // never close the manager this is done by the service
    }
}
