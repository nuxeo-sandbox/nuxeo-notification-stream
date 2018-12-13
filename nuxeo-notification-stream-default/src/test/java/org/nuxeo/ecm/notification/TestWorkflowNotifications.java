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
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.notification.TestNotificationHelper.waitProcessorsCompletion;
import static org.nuxeo.ecm.notification.message.Notification.ORIGINATING_USER;
import static org.nuxeo.ecm.notification.resolver.WorkflowUpdatesResolver.RESOLVER_NAME;
import static org.nuxeo.ecm.notification.resolver.WorkflowUpdatesResolver.WORKFLOW_ID_KEY;
import static org.nuxeo.runtime.stream.StreamHelper.drainAndStop;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.notifier.CounterNotifier;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRoutingService;
import org.nuxeo.ecm.platform.routing.core.api.DocumentRoutingEngineService;
import org.nuxeo.ecm.platform.routing.core.impl.GraphRoute;
import org.nuxeo.ecm.platform.routing.test.WorkflowFeature;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Test the notifications sent when updates occur on a workflow.
 *
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features({ NotificationFeature.class, WorkflowFeature.class })
@Deploy("org.nuxeo.ecm.platform.routing.default")
@Deploy("org.nuxeo.ecm.platform.filemanager.api")
@Deploy("org.nuxeo.ecm.platform.filemanager.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.default")
@Deploy("org.nuxeo.ecm.platform.notification.stream.default:OSGI-INF/default-contrib.xml")
public class TestWorkflowNotifications {

    protected static final String USER_ID = "user1";

    @Inject
    protected CoreSession session;

    @Inject
    protected DocumentRoutingService routingService;

    @Inject
    protected DocumentRoutingEngineService routingEngineService;

    @Inject
    protected TaskService taskService;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected NotificationStreamCallback ncs;

    @Inject
    protected NotificationService ns;

    protected static final String WORKFLOW_ID = "SerialDocumentReview";

    protected String routeId;

    @Before
    public void setup() {
        // Create a new user
        DocumentModel user = userManager.getBareUserModel();
        user.setPropertyValue(userManager.getUserIdField(), USER_ID);
        userManager.createUser(user);

        // Add permission to create document
        PathRef rootRef = new PathRef("/");
        ACP acp = session.getACP(rootRef);
        ACL existingACL = acp.getOrCreateACL();
        existingACL.add(new ACE("user1", EVERYTHING, true));
        session.setACP(new PathRef("/"), acp, true);

        // Wait for all asynchronous processes
        waitEndAsyncProcesses();
    }

    @Test
    public void testSubscriptionsWorkflowResolver() {
        // Create a new Document and start of workflow
        createDocumentAndStartWorkflow();

        // Subscribe user1 to the workflow
        assertThat(ns.getSubscriptions(RESOLVER_NAME, Collections.singletonMap(WORKFLOW_ID_KEY, routeId))
                     .getUsernames()).containsExactlyInAnyOrder("Administrator", "user1");
    }

    @Test
    public void notificationTriggeredWhenWorkflowIsCanceled() {
        // Create a new Document and start of workflow
        createDocumentAndStartWorkflow();

        // Cancel the workflow and check the notifications
        DocumentModel documentRoute = session.getDocument(new IdRef(routeId));
        routingEngineService.cancel(documentRoute.getAdapter(DocumentRoute.class), session);
        waitEndAsyncProcesses();

        assertThat(CounterNotifier.processed).isEqualTo(1);
        assertThat(CounterNotifier.getLast().getResolverId()).isEqualTo(RESOLVER_NAME);
        assertThat(CounterNotifier.getLast().getUsername()).isEqualTo(USER_ID);
    }

    @Test
    public void notificationTriggeredWhenWorkflowIsCompleted() {
        // Create a new Document and start of workflow
        createDocumentAndStartWorkflow();

        // Complete the task to finish the workflow
        List<Task> listTasks = taskService.getCurrentTaskInstances(session);
        routingService.endTask(session, listTasks.get(0), Collections.emptyMap(), "cancel");
        waitEndAsyncProcesses();

        // Two notifications are sent, one for the completed task and another for the end of the workflow
        assertThat(CounterNotifier.processed).isEqualTo(2);
        Notification notification = CounterNotifier.getLast();
        assertThat(notification.getResolverId()).isEqualTo(RESOLVER_NAME);
        assertThat(notification.getUsername()).isEqualTo(USER_ID);
        assertThat(notification.getContext().get(ORIGINATING_USER)).isEqualTo("Administrator");
    }

    @Test
    public void notificationTriggeredWhenTaskIsCompleted() {
        // Create a new Document and start of workflow
        createDocumentAndStartWorkflow();

        // Set the participants for the workflow
        DocumentModel workflowInstance = session.getDocument(new IdRef(routeId));
        GraphRoute graph = workflowInstance.getAdapter(GraphRoute.class);
        Map<String, Serializable> vars = graph.getVariables();
        vars.put("participants",  (Serializable) Arrays.asList("user2"));
        graph.setVariables(vars);

        // Complete a task that will not end the workflow
        List<Task> listTasks = taskService.getCurrentTaskInstances(session);
        routingService.endTask(session, listTasks.get(0), Collections.emptyMap(), "start_review");
        waitEndAsyncProcesses();

        // Check that a notification is sent to user1
        List<Notification> notifUser1 = CounterNotifier.fullCtx.stream().filter(n -> n.getUsername().equals("user1")).collect(Collectors.toList());
        assertThat(notifUser1).hasSize(1);
        Notification notification = notifUser1.get(0);
        assertThat(notification.getResolverId()).isEqualTo(RESOLVER_NAME);
        assertThat(notification.getUsername()).isEqualTo(USER_ID);
        assertThat(notification.getContext().get(ORIGINATING_USER)).isEqualTo("Administrator");
    }

    protected void waitEndAsyncProcesses() {
        session.save();
        txFeature.nextTransaction();
        drainAndStop();
    }

    protected void createDocumentAndStartWorkflow() {
        // Create a new Document
        DocumentModel doc = session.createDocumentModel("/", "TestDoc", "File");
        doc = session.createDocument(doc);
        session.save();
        txFeature.nextTransaction();
        waitProcessorsCompletion();

        // Start a new workflow on it
        routeId = routingService.createNewInstance(WORKFLOW_ID, Collections.singletonList(doc.getId()),
                Collections.emptyMap(), session, true);
        txFeature.nextTransaction();
        waitProcessorsCompletion();

        // Subscribe the other user to the updates
        ncs.doSubscribe(USER_ID, RESOLVER_NAME, Collections.singletonMap(WORKFLOW_ID_KEY, routeId));
    }
}
