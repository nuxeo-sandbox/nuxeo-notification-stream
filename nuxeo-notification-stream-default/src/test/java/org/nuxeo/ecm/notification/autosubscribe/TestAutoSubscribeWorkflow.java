/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.autosubscribe;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.notification.TestNotificationHelper.waitProcessorsCompletion;
import static org.nuxeo.ecm.notification.TestNotificationHelper.withUser;
import static org.nuxeo.ecm.notification.resolver.WorkflowUpdatesResolver.RESOLVER_NAME;
import static org.nuxeo.ecm.notification.resolver.WorkflowUpdatesResolver.WORKFLOW_ID_KEY;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test the auto-subscription of the initiator of a workflow to all its updates.
 *
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features({ NotificationFeature.class, WorkflowFeature.class })
@Deploy("org.nuxeo.ecm.platform.routing.default")
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.default")
public class TestAutoSubscribeWorkflow extends AbstractTestAutoSubscribe {

    @Inject
    protected DocumentRoutingService routingService;

    @Inject
    protected EventService eventService;

    protected static final String WORKFLOW_ID = "SerialDocumentReview";

    @Test
    public void noAutoSubscribeWhenWorkflowIsStartedBySystem() {
        String docId;
        // Create a new document as system
        try (CloseableCoreSession s = CoreInstance.openCoreSessionSystem(session.getRepositoryName())) {
            DocumentModel doc = s.createDocumentModel("/", "TestDoc", "File");
            doc = s.createDocument(doc);
            docId = doc.getId();
            s.save();

            txFeature.nextTransaction();
            waitProcessorsCompletion();

            // Start a new workflow on the created document
            docId = routingService.createNewInstance(WORKFLOW_ID, Arrays.asList(docId), Collections.emptyMap(), s,
                    true);
            txFeature.nextTransaction();
            waitProcessorsCompletion();
        }

        // Check if there is a subscription for the created doc
        String routeName = session.getDocument(new IdRef(docId)).getTitle();
        assertThat(ns.getSubscriptions(RESOLVER_NAME, singletonMap(WORKFLOW_ID_KEY, routeName))
                     .getUsernames()).isEmpty();

    }

    @Test
    public void testAutoSubscribeWhenWorkflowIsStartedByAdmin() {
        DocumentModel doc = session.createDocumentModel("/", "TestDoc", "File");
        doc = session.createDocument(doc);
        session.save();

        // Start a new workflow on the created document
        String routeId = routingService.createNewInstance(WORKFLOW_ID, Arrays.asList(doc.getId()),
                Collections.emptyMap(), session, true);

        txFeature.nextTransaction();
        waitProcessorsCompletion();

        // Check that the user has been subscribed to the updates on the workflow
        checkSubscription(singletonMap(WORKFLOW_ID_KEY, routeId), RESOLVER_NAME, "Administrator");
    }

    @Test
    public void testAutoSubscribeWhenWorkflowIsStarted() {
        // Create a new user
        createUserAndAddPermissionsToCreateDocuments("user1");

        // Create a new user as user1 and start a workflow on it
        String[] routeId = new String[1];
        withUser("user1", session.getRepositoryName(), (s) -> {
            DocumentModel doc = s.createDocumentModel("/", "TestDoc", "File");
            doc = s.createDocument(doc);
            s.save();

            txFeature.nextTransaction();
            waitProcessorsCompletion();

            // Start a new workflow on the created document
            routeId[0] = routingService.createNewInstance(WORKFLOW_ID, Arrays.asList(doc.getId()),
                    Collections.emptyMap(), s, true);
            txFeature.nextTransaction();
            waitProcessorsCompletion();
        });

        // Check that the user has been subscribed to the updates on the workflow
        checkSubscription(singletonMap(WORKFLOW_ID_KEY, routeId[0]), RESOLVER_NAME, "user1");
    }
}
