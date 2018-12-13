/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.transformer;

import static org.nuxeo.ecm.notification.resolver.WorkflowUpdatesResolver.ACCEPTED_EVENTS;
import static org.nuxeo.ecm.notification.resolver.WorkflowUpdatesResolver.WORKFLOW_ID_KEY;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.Events.afterWorkflowTaskEnded;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.Events.workflowCanceled;
import static org.nuxeo.ecm.platform.routing.core.impl.DocumentRoutingEngineServiceImpl.WORKFLOW_ID_EVENT_PROPERTY_KEY;

import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.task.Task;

public class WorkflowEventTransformer extends EventTransformer {

    @Override
    public boolean accept(Event event) {
        return ACCEPTED_EVENTS.contains(event.getName());
    }

    @Override
    public Map<String, String> buildEventRecordContext(Event event) {
        // Extract the route instance id from the properties of the event if the event is canceled.
        String wfId;
        if (event.getName().equals(workflowCanceled.name())) {
            wfId = (String) event.getContext().getProperties().get(WORKFLOW_ID_EVENT_PROPERTY_KEY);
        } else if (event.getName().equals(afterWorkflowTaskEnded.name())) {
            // Extract the task to fetch the workflow title
            DocumentModel taskDoc = ((DocumentEventContext) event.getContext()).getSourceDocument();
            Task task = taskDoc.getAdapter(Task.class);
            wfId = task.getProcessId();
        } else {
            // Otherwise, get the title from the attached source document
            wfId = ((DocumentEventContext) event.getContext()).getSourceDocument().getId();
        }
        return Collections.singletonMap(WORKFLOW_ID_KEY, wfId);
    }
}
