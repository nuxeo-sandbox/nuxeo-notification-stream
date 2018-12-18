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

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.notifier.CounterNotifier;
import org.nuxeo.ecm.platform.task.Task;
import org.nuxeo.ecm.platform.task.TaskService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.stream.StreamHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@RunWith(FeaturesRunner.class)
@Features({ NotificationFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.ecm.platform.task.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.default")
@Deploy("org.nuxeo.ecm.platform.notification.stream.default:OSGI-INF/default-contrib.xml")
public class TestTaskAssignedNotifications {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected TaskService taskService;

    protected static final String TEST_USER_1 = "user1";

    protected static final String TEST_USER_2 = "user2";

    protected static final String TEST_USER_3 = "user3";

    @Before
    public void setup() {
        // Create the test users
        DocumentModel user1 = userManager.getBareUserModel();
        user1.setPropertyValue(userManager.getUserIdField(), TEST_USER_1);
        userManager.createUser(user1);
        DocumentModel user2 = userManager.getBareUserModel();
        user2.setPropertyValue(userManager.getUserIdField(), TEST_USER_2);
        userManager.createUser(user2);
        DocumentModel user3 = userManager.getBareUserModel();
        user3.setPropertyValue(userManager.getUserIdField(), TEST_USER_3);
        userManager.createUser(user3);
    }

    @Test
    public void testNotificationWhenTaskIsAssigned() {
        // Create a new document and a new task on the document
        DocumentModel doc = createDocument();
        // Create a task on the document
        String taskId = createTask(doc, Arrays.asList(TEST_USER_1)).get(0).getId();

        // Check the notifications created
        assertThat(CounterNotifier.processed).isEqualTo(1);
        Notification notification = CounterNotifier.getLast();
        assertThat(notification.getSourceId()).isEqualTo(doc.getId());
        assertThat(notification.getUsername()).isEqualTo(TEST_USER_1);
        assertThat(notification.getResolverId()).isEqualTo("taskAssigned");
        assertThat(notification.getMessage()).isEqualTo(String.format(
                "The task @{doc:%s} on document @{doc:%s} has been assigned to you.", taskId, doc.getId()));
    }

    @Test
    public void testNotificationWhenTaskIsReassigned() {
        // Create a new document and a new task on the document
        DocumentModel doc = createDocument();
        // Create a task on the document
        List<Task> tasks = createTask(doc, Arrays.asList(TEST_USER_1));

        // Reset the counter of notification
        CounterNotifier.reset();

        // Reassign the task
        String taskId = tasks.get(0).getId();
        taskService.reassignTask(session, taskId, Arrays.asList(TEST_USER_2, TEST_USER_3), "Comment");
        session.save();
        txFeature.nextTransaction();
        assertThat(StreamHelper.drainAndStop()).isTrue();

        assertThat(CounterNotifier.processed).isEqualTo(2);
        List<Notification> notifications = CounterNotifier.fullCtx;
        assertThat(
                notifications.stream()
                             .allMatch(n -> n.getUsername().equals(TEST_USER_2) || n.getUsername().equals(TEST_USER_3)))
                                                                                                                .isTrue();
        assertThat(notifications.stream().allMatch(n -> n.getSourceId().equals(doc.getId()))).isTrue();
        String expectedMessage = String.format("The task @{doc:%s} on document @{doc:%s} has been assigned to you.",
                taskId, doc.getId());
        assertThat(notifications.stream().allMatch(n -> n.getMessage().equals(expectedMessage))).isTrue();
    }

    @Test
    public void testNotificationWhenTaskIsDelegated() {
        // Create a new document and a new task on the document
        DocumentModel doc = createDocument();
        // Create a task on the document
        List<Task> tasks = createTask(doc, Arrays.asList(TEST_USER_1));

        // Reset the counter of notification
        CounterNotifier.reset();

        // Delegate the task
        String taskId = tasks.get(0).getId();
        taskService.delegateTask(session, taskId, Arrays.asList(TEST_USER_2, TEST_USER_3), "Comment");
        session.save();
        txFeature.nextTransaction();
        assertThat(StreamHelper.drainAndStop()).isTrue();

        assertThat(CounterNotifier.processed).isEqualTo(2);
        List<Notification> notifications = CounterNotifier.fullCtx;
        assertThat(
                notifications.stream()
                             .allMatch(n -> n.getUsername().equals(TEST_USER_2) || n.getUsername().equals(TEST_USER_3)))
                                                                                                                .isTrue();
        assertThat(notifications.stream().allMatch(n -> n.getSourceId().equals(doc.getId()))).isTrue();
        String expectedMessage = String.format("The task @{doc:%s} on document @{doc:%s} has been assigned to you.",
                taskId, doc.getId());
        assertThat(notifications.stream().allMatch(n -> n.getMessage().equals(expectedMessage))).isTrue();

    }

    protected DocumentModel createDocument() {
        DocumentModel doc = session.createDocumentModel(session.getRootDocument().getPathAsString(), "TestFile",
                "File");
        doc = session.createDocument(doc);
        session.save();

        return doc;
    }

    protected List<Task> createTask(DocumentModel doc, List<String> users) {
        List<Task> tasks = taskService.createTask(session, session.getPrincipal(), doc, "Task assigned to user1", users,
                false, null, null, null, null, null);
        session.save();
        txFeature.nextTransaction();
        assertThat(StreamHelper.drainAndStop()).isTrue();

        return tasks;
    }
}
