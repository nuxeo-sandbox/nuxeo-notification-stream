/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.notification.transformer.TaskEventTransformer.TASK_ID_KEY;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_NEW_STARTED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_ASSIGNED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_COMPLETED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_DELEGATED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_REASSIGNED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_START;
import static org.nuxeo.ecm.platform.task.TaskService.TASK_INSTANCE_EVENT_PROPERTIES_KEY;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.platform.task.TaskImpl;

/**
 * Test class for the event transformer {@link TaskEventTransformer}.
 *
 * @since XXX
 */
public class TestTaskEventTransformer {

    @Test
    public void testTransformerAccept() {
        TaskEventTransformer transformer = new TaskEventTransformer();
        assertThat(transformer.accept(new EventImpl(WORKFLOW_TASK_ASSIGNED, null))).isTrue();
        assertThat(transformer.accept(new EventImpl(WORKFLOW_TASK_REASSIGNED, null))).isTrue();
        assertThat(transformer.accept(new EventImpl(WORKFLOW_TASK_DELEGATED, null))).isTrue();
        assertThat(transformer.accept(new EventImpl(WORKFLOW_TASK_COMPLETED, null))).isFalse();
        assertThat(transformer.accept(new EventImpl(WORKFLOW_TASK_START, null))).isFalse();
        assertThat(transformer.accept(new EventImpl(WORKFLOW_NEW_STARTED, null))).isFalse();
    }

    @Test
    public void testTransformerBuildContext() {
        TaskEventTransformer transformer = new TaskEventTransformer();

        // Test the context builder with an IdRef of a Collection
        Map<String, Serializable> props = new HashMap<>();
        props.put(RECIPIENTS_KEY, new String[] { "user1", "user2" });
        DocumentModel doc = Mockito.mock(DocumentModel.class);
        Mockito.when(doc.getId()).thenReturn("0000-1111");
        props.put(TASK_INSTANCE_EVENT_PROPERTIES_KEY, new TaskImpl(doc));
        EventContext eventCtx = new EventContextImpl();
        eventCtx.setProperties(props);

        // Check the built context
        Map<String, String> ctx = transformer.buildEventRecordContext(new EventImpl(WORKFLOW_TASK_ASSIGNED, eventCtx));
        assertThat(ctx).hasSize(2);
        assertThat(ctx.get(RECIPIENTS_KEY)).isEqualTo("user1:user2");
        assertThat(ctx.get(TASK_ID_KEY)).isEqualTo("0000-1111");
    }
}
