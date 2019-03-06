/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.autosubscribe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.notification.TestNotificationHelper.waitProcessorsCompletion;
import static org.nuxeo.ecm.notification.TestNotificationHelper.withUser;
import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_DOC_ID;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.RESOLVER_NAME;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.model.Subscribers;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test class to validate the autosubscribe to the updates of a document at its creation.
 *
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ NotificationFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.ecm.platform.notification.stream.default")
public class TestAutoSubscribeAtCreation extends AbstractTestAutoSubscribe {

    @Test
    public void noSubscriptionWhenDocIsCreatedAsSystem() {
        String docId;
        // Create a new document as system
        try (CloseableCoreSession coreSession = CoreInstance.openCoreSessionSystem(session.getRepositoryName())) {
            DocumentModel doc = coreSession.createDocumentModel("/", "TestDoc", "File");
            doc = coreSession.createDocument(doc);
            docId = doc.getId();
            coreSession.save();
        }

        txFeature.nextTransaction();
        waitProcessorsCompletion();

        // Check if there is a subscription for the created doc
        Map<String, String> ctx = new HashMap<>();
        ctx.put(SOURCE_DOC_ID, docId);
        assertThat(ns.getSubscriptions(RESOLVER_NAME, ctx).getUsernames()).isEmpty();
    }

    @Test
    public void noSubscriptionWhenDocIsCreatedByAdministrators() {
        // Create a new document as an Admin
        DocumentModel doc = session.createDocumentModel("/", "TestDoc", "File");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
        waitProcessorsCompletion();

        // Check if there is a subscription for the created doc
        Map<String, String> ctx = new HashMap<>();
        ctx.put(SOURCE_DOC_ID, doc.getId());
        assertThat(ns.getSubscriptions(RESOLVER_NAME, ctx).getUsernames()).isEmpty();
    }

    @Test
    public void autoSubscriptionWhenDocIsCreatedByUser() {
        createUserAndAddPermissionsToCreateDocuments("user1");

        // Create a new document as a user
        withUser("user1", session.getRepositoryName(), (s) -> {
            DocumentModel doc = s.createDocumentModel("/", "TestDoc", "File");
            s.createDocument(doc);
            s.save();
            txFeature.nextTransaction();
            waitProcessorsCompletion();
        });

        // Check if there is a subscription created for "user1"
        DocumentModel createdDoc = session.getDocument(new PathRef("/TestDoc"));
        Map<String, String> ctx = new HashMap<>();
        ctx.put(SOURCE_DOC_ID, createdDoc.getId());
        Subscribers subscribers = ns.getSubscriptions(RESOLVER_NAME, ctx);
        assertThat(subscribers.getUsernames()).hasSize(1);
        assertThat(subscribers.getUsernames()).containsOnly("user1");

        // Copy the document and check that the user is subscribed to it too
        withUser("user1", session.getRepositoryName(), (s) -> {
            s.copy(new PathRef("/TestDoc"), new PathRef("/"), "CopyDoc");
            s.save();
            txFeature.nextTransaction();
            waitProcessorsCompletion();
        });
        // Check if there is a subscription created for the copied document
        DocumentModel copiedDoc = session.getDocument(new PathRef("/CopyDoc"));
        ctx = new HashMap<>();
        ctx.put(SOURCE_DOC_ID, copiedDoc.getId());
        subscribers = ns.getSubscriptions(RESOLVER_NAME, ctx);
        assertThat(subscribers.getUsernames()).hasSize(1);
        assertThat(subscribers.getUsernames()).containsOnly("user1");
    }

    protected void createUserAndAddPermissionsToCreateDocuments(String username) {
        // Create a new user
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setProperty("user", "username", "user1");
        userManager.createUser(newUser);

        // Add permission to the user to create a new document
        PathRef rootRef = new PathRef("/");
        ACP acp = session.getACP(rootRef);
        ACL existingACL = acp.getOrCreateACL();
        existingACL.add(new ACE("user1", EVERYTHING, true));
        session.setACP(new PathRef("/"), acp, true);
    }
}
