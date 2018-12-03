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
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.COMMENT_AUTHOR_KEY;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.COMMENT_ID_KEY;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.DOC_ID_KEY;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.RESOLVER_NAME;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
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
@Deploy("org.nuxeo.ecm.platform.comment.api")
@Deploy("org.nuxeo.ecm.platform.comment")
@Deploy("org.nuxeo.ecm.platform.notification.stream.default")
public class TestDocumentUpdatedNotifications {

    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationService notificationService;

    @Inject
    protected NotificationStreamConfig streamConfig;

    @Inject
    protected NotificationStreamCallback scb;

    @Inject
    protected CommentManager commentManager;

    @Test
    public void resolverIsAvailable() {
        assertThat(notificationService.getResolver(RESOLVER_NAME)).isNotNull();
    }

    @Test
    public void userCanSubscribeToResolver() {
        // Create a new Document and subscribe a user to it
        DocumentModel doc = createDocumentAndSubscribeUsers("user1");

        // Context for checking the subscriptions
        Map<String, String> ctx = new HashMap<>();
        ctx.put(DOC_ID_KEY, doc.getId());

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
        ctx.put(DOC_ID_KEY, "0000");
        scb.doSubscribe("user1", RESOLVER_NAME, ctx);
        scb.doSubscribe("user2", RESOLVER_NAME, ctx);
        scb.doSubscribe("user3", RESOLVER_NAME, ctx);

        assertThat(notificationService.getSubscriptions(RESOLVER_NAME, ctx).getUsernames()).containsExactlyInAnyOrder(
                "user1", "user2", "user3");
    }

    @Test
    public void notificationIsCreated() throws InterruptedException {
        // Create a new document and subscribe to the resolver for this document
        DocumentModel doc = createDocumentAndSubscribeUsers("user1");

        // Update the document and check if a notification record has been created
        doc = session.getDocument(doc.getRef());
        doc.setPropertyValue("dc:title", "New Title");
        session.saveDocument(doc);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        TestNotificationHelper.waitProcessorsCompletion();

        // Check the notification stream
        Record record = readRecord(streamConfig.getNotificationOutputStream(),
                streamConfig.getNotificationOutputStream());
        Codec<Notification> codec = Framework.getService(CodecService.class)
                                             .getCodec(DEFAULT_CODEC, Notification.class);
        Notification notification = codec.decode(record.getData());
        assertThat(notification.getUsername()).isEqualTo("user1");
        assertThat(notification.getSourceId()).isEqualTo(doc.getId());
        assertThat(notification.getSourceRepository()).isEqualTo(doc.getRepositoryName());
    }

    @Test
    public void notificationIsCreatedWhenCommentIsCreated() throws InterruptedException {
        // Create a new document and subscribe to the resolver for this document
        DocumentModel doc = createDocumentAndSubscribeUsers("user1");

        // Create a comment on the document
        Comment comment = new CommentImpl();
        comment.setParentId(doc.getId());
        comment.setAuthor("user2");
        comment.setText("Test comment");
        comment = commentManager.createComment(session, comment);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        TestNotificationHelper.waitProcessorsCompletion();

        // Check the notification stream
        Record record = readRecord(streamConfig.getNotificationOutputStream(),
                streamConfig.getNotificationOutputStream());
        Codec<Notification> codec = Framework.getService(CodecService.class)
                                             .getCodec(DEFAULT_CODEC, Notification.class);
        Notification notification = codec.decode(record.getData());
        assertThat(notification.getUsername()).isEqualTo("user1");
        assertThat(notification.getSourceId()).isEqualTo(comment.getId());
        assertThat(notification.getSourceRepository()).isEqualTo(doc.getRepositoryName());
        assertThat(notification.getUsername()).isEqualTo("user1");
        assertThat(notification.getContext().get(DOC_ID_KEY)).isEqualTo(doc.getId());
        assertThat(notification.getContext().get(COMMENT_ID_KEY)).isEqualTo(comment.getId());
        assertThat(notification.getContext().get(COMMENT_AUTHOR_KEY)).isEqualTo("Administrator");
    }

    protected DocumentModel createDocumentAndSubscribeUsers(String user) {
        DocumentModel doc = session.createDocumentModel("/", "Test", "File");
        doc = session.createDocument(doc);
        session.save();
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        TestNotificationHelper.waitProcessorsCompletion();

        // User subscribe to the new document
        Map<String, String> ctx = new HashMap<>();
        ctx.put(DOC_ID_KEY, doc.getId());
        scb.doSubscribe(user, RESOLVER_NAME, ctx);
        TestNotificationHelper.waitProcessorsCompletion();

        return doc;
    }
}
