/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.listener;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.notification.NotificationService;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * Asynchronous events listener that pushed all the contributed events into a Log to feed the Notification Topology.
 *
 * @since XXX
 */
public class EventsStreamListener implements PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle eventBundle) {
        eventBundle.forEach(this::processEvent);
    }

    /**
     * Extract the event and push it into the Log configured.
     *
     * @param event The raised event.
     */
    protected void processEvent(Event event) {
        // Get the stream where to publish the event
        NotificationService notificationService = Framework.getService(NotificationService.class);
        String eventStream = notificationService.getEventInputStream();

        if (StringUtils.isEmpty(eventStream)) {
            throw new NuxeoException("There is no Stream configured to publish the event record.");
        }

        // Create a record in the stream in input of the notification processor
        LogManager logManager = notificationService.getLogManager();
        LogAppender<Record> appender = logManager.getAppender(notificationService.getEventInputStream());
        byte[] encodedEvent = encodeEvent(event);

        // Append the record to the log
        String key = event.getName();
        appender.append(eventStream, Record.of(key, encodedEvent));
    }

    /**
     * Encode the Event using the Avro codec to sent it to the Stream.
     *
     * @param event
     * @return
     */
    protected byte[] encodeEvent(Event event) {
        EventRecord record = new EventRecord(event.getName(), event.getContext().getPrincipal().getName());

        if (event.getContext() instanceof DocumentEventContext) {
            String docId = ((DocumentEventContext) event.getContext()).getSourceDocument().getId();
            record.setDocumentSourceId(docId);
        }

        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, EventRecord.class).encode(record);
    }
}
