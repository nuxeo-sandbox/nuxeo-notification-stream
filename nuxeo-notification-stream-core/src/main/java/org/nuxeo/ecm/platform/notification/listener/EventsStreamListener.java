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
import org.nuxeo.ecm.platform.notification.NotificationStreamConfig;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.ecm.platform.notification.message.EventRecord.EventRecordBuilder;
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

    public static final String FORMAT_KEY = "%s:%s";

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
        NotificationStreamConfig notificationService = Framework.getService(NotificationStreamConfig.class);
        String eventStream = notificationService.getEventInputStream();

        if (StringUtils.isEmpty(eventStream)) {
            throw new NuxeoException("There is no Stream configured to publish the event record.");
        }

        // Create a record in the stream in input of the notification processor
        LogManager logManager = notificationService.getLogManager(notificationService.getLogConfigNotification());
        LogAppender<Record> appender = logManager.getAppender(notificationService.getEventInputStream());
        byte[] encodedEvent = encodeEvent(event);

        // Append the record to the log
        String key = generateKey(event);
        appender.append(eventStream, Record.of(key, encodedEvent));
    }

    /**
     * Encode the Event using the Avro codec to sent it to the Stream.
     *
     * @param event
     * @return The encoded event.
     */
    protected byte[] encodeEvent(Event event) {
        EventRecordBuilder builder = EventRecord.builder().withEventName(event.getName()).withUsername(
                event.getContext().getPrincipal().getName());

        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext ctx = (DocumentEventContext) event.getContext();
            builder.withDocument(ctx.getSourceDocument());
        }

        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, EventRecord.class).encode(
                builder.build());
    }

    /**
     * Generates the key for the record that will be appended to the input stream of the topology.
     * 
     * @param event
     * @return A String representing the key.
     */
    protected String generateKey(Event event) {
        if (event.getContext() instanceof DocumentEventContext) {
            return String.format(FORMAT_KEY, event.getName(),
                    ((DocumentEventContext) event.getContext()).getSourceDocument().getId());
        } else {
            return String.format(FORMAT_KEY, event.getName(), event.getContext().getPrincipal().getName());
        }
    }
}
