/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.Events.afterWorkflowFinish;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.Events.afterWorkflowTaskDelegated;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.Events.afterWorkflowTaskEnded;
import static org.nuxeo.ecm.platform.routing.api.DocumentRoutingConstants.Events.workflowCanceled;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Resolver to notify the initiator of a workflow of the different updates triggered during its process. It includes
 * notifications on tasks started, completed or canceled, cancellation of the workflow and end of the workflow.
 *
 * @since XXX
 */
public class WorkflowUpdatesResolver extends SubscribableResolver {

    public static final String RESOLVER_NAME = "workflow";

    public static final List<String> ACCEPTED_EVENTS = Arrays.asList(workflowCanceled.name(),
            afterWorkflowFinish.name(), afterWorkflowTaskEnded.name());

    public static final String WORKFLOW_ID_KEY = "wfId";

    public static final String CTX_ACTION = "action";

    public static final String CTX_TARGET = "target";

    public static final String CTX_TARGET_ID = "targetId";

    @Override
    public List<String> getRequiredContextFields() {
        return Collections.singletonList(WORKFLOW_ID_KEY);
    }

    @Override
    public boolean accept(EventRecord eventRecord) {
        return ACCEPTED_EVENTS.contains(eventRecord.getEventName());
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        Map<String, String> ctx =  new HashMap<>();
        ctx.put(WORKFLOW_ID_KEY, eventRecord.getContext().get(WORKFLOW_ID_KEY));
        // Add the context info for the message
        if (eventRecord.getEventName().equals(afterWorkflowTaskEnded.name())) {
            ctx.put(CTX_TARGET, "task");
            ctx.put(CTX_ACTION, "completed");
            ctx.put(CTX_TARGET_ID, eventRecord.getDocumentSourceId());
        } else {
            ctx.put(CTX_TARGET, "workflow");
            ctx.put(CTX_TARGET_ID, eventRecord.getContext().get(WORKFLOW_ID_KEY));
            ctx.put(CTX_ACTION, eventRecord.getEventName().equals(workflowCanceled.name()) ? "canceled" : "completed");
        }
        return ctx;
    }
}
