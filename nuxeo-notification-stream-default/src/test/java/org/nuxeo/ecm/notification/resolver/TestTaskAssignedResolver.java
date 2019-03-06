/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.collections.api.CollectionConstants.BEFORE_ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.BEFORE_REMOVED_FROM_COLLECTION;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_ASSIGNED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_DELEGATED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_REASSIGNED;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Test class for the resolver {@link TaskAssignedResolver}.
 *
 * @since 0.1
 */
public class TestTaskAssignedResolver {

    @Test
    public void resolverOnlyAcceptsTasksEvents() {
        Resolver resolver = new TaskAssignedResolver();
        EventRecord.EventRecordBuilder builder = EventRecord.builder().withEventName(WORKFLOW_TASK_ASSIGNED);
        assertThat(resolver.accept(builder.build())).isTrue();
        assertThat(resolver.accept(builder.withEventName(WORKFLOW_TASK_REASSIGNED).build())).isTrue();
        assertThat(resolver.accept(builder.withEventName(WORKFLOW_TASK_DELEGATED).build())).isTrue();
        assertThat(resolver.accept(builder.withEventName(BEFORE_ADDED_TO_COLLECTION).build())).isFalse();
        assertThat(resolver.accept(builder.withEventName(BEFORE_REMOVED_FROM_COLLECTION).build())).isFalse();
    }

    @Test
    public void resolverReturnsTargetUsers() {
        Resolver resolver = new TaskAssignedResolver();
        // Create an event record without any recipients and check the target users
        EventRecord.EventRecordBuilder builder = EventRecord.builder().withEventName(WORKFLOW_TASK_ASSIGNED);
        assertThat(resolver.resolveTargetUsers(builder.build())).isNull();

        // Add the key in the context without any values
        Map<String, String> ctx = new HashMap<>();
        ctx.put(RECIPIENTS_KEY, null);
        assertThat(resolver.resolveTargetUsers(builder.withContext(ctx).build())).isNull();

        // Add one recipient
        ctx.put(RECIPIENTS_KEY, "user1");
        assertThat(resolver.resolveTargetUsers(builder.withContext(ctx).build())).containsOnly("user1");
        ctx.put(RECIPIENTS_KEY, ":user1:");
        assertThat(resolver.resolveTargetUsers(builder.withContext(ctx).build())).containsOnly("user1");

        // Add several recipients
        ctx.put(RECIPIENTS_KEY, "user1:user2:user3:user4");
        assertThat(resolver.resolveTargetUsers(builder.withContext(ctx).build())).containsOnly("user1", "user2",
                "user3", "user4");
    }
}
