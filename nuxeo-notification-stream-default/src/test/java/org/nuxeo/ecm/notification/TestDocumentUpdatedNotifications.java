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
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.DOC_ID_KEY;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.EVENT_KEY;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.RESOLVER_NAME;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import javax.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.notification.message.Notification;
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
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test to validate the resolver triggered when a document is updated.
 *
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.default")
public class TestDocumentUpdatedNotifications {

    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationService notificationService;

    @Inject
    protected NotificationStreamConfig streamConfig;

    @Test
    public void notifierIsAvailable() {
        assertThat(notificationService.getResolver(RESOLVER_NAME)).isNotNull();
    }

    @Test
    public void userCanSubscribeToResolver() {
        // Create a new Document
        DocumentModel doc = session.createDocumentModel("/", "Test", "File");
        doc = session.createDocument(doc);
        session.save();

        // User subscribe to the new document
        Map<String, String> ctx = new HashMap<>();
        ctx.put(EVENT_KEY, DOCUMENT_UPDATED);
        ctx.put(DOC_ID_KEY, doc.getId());
        notificationService.subscribe("user1", RESOLVER_NAME, ctx);
        TestNotificationHelper.waitProcessorsCompletion();

        // Check if the user has properly subscribed to the resolver
        assertThat(notificationService.hasSubscribe("user1", RESOLVER_NAME, ctx)).isTrue();
        assertThat(notificationService.hasSubscribe("user2", RESOLVER_NAME, ctx)).isFalse();

        // Unsubscribe the user to the resolver
        notificationService.unsubscribe("user1", RESOLVER_NAME, ctx);
        TestNotificationHelper.waitProcessorsCompletion();
        assertThat(notificationService.hasSubscribe("user1", RESOLVER_NAME, ctx)).isFalse();
    }

    @Test
    public void resolverReturnsListTargetUsers() {
        // Subscribe a few users to the resolver for a fake document
        Map<String, String> ctx = new HashMap<>();
        ctx.put(EVENT_KEY, DOCUMENT_UPDATED);
        ctx.put(DOC_ID_KEY, "0000");
        notificationService.subscribe("user1", RESOLVER_NAME, ctx);
        notificationService.subscribe("user2", RESOLVER_NAME, ctx);
        notificationService.subscribe("user3", RESOLVER_NAME, ctx);
        TestNotificationHelper.waitProcessorsCompletion();

        assertThat(notificationService.getSubscriptions(RESOLVER_NAME, ctx).getUsernames()).containsExactlyInAnyOrder(
                "user1", "user2", "user3");
    }

    @Test
    public void notificationIsCreated() throws InterruptedException {
        // Create a new document and subscribe to the resolver for this document
        DocumentModel doc = session.createDocumentModel("/", "Test", "File");
        doc = session.createDocument(doc);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        TestNotificationHelper.waitProcessorsCompletion();

        // User subscribe to the new document
        Map<String, String> ctx = new HashMap<>();
        ctx.put(EVENT_KEY, DOCUMENT_UPDATED);
        ctx.put(DOC_ID_KEY, doc.getId());
        notificationService.subscribe("user1", RESOLVER_NAME, ctx);
        TestNotificationHelper.waitProcessorsCompletion();

        // Update the document and check if a notification record has been created
        doc = session.getDocument(doc.getRef());
        doc.setPropertyValue("dc:title", "New Title");
        session.saveDocument(doc);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        TestNotificationHelper.waitProcessorsCompletion();

        // Check the notification stream
        Record record = readRecord();
        Codec<Notification> codec = Framework.getService(CodecService.class)
                                             .getCodec(DEFAULT_CODEC, Notification.class);
        Notification notification = codec.decode(record.getData());
        assertThat(notification.getUsername()).isEqualTo("user1");
        assertThat(notification.getSourceId()).isEqualTo(doc.getId());
        assertThat(notification.getSourceRepository()).isEqualTo(doc.getRepositoryName());
    }

    protected Record readRecord() throws InterruptedException {
        // Check the record in the stream
        Codec<Record> codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, Record.class);
        LogManager logManager = streamConfig.getLogManager(streamConfig.getLogConfigNotification());
        try (LogTailer<Record> tailer = logManager.createTailer(streamConfig.getNotificationOutputStream(),
                streamConfig.getNotificationOutputStream(), codec)) {
            LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(5));
            tailer.commit();
            return logRecord.message();
        }
        // never close the manager this is done by the service
    }
}
