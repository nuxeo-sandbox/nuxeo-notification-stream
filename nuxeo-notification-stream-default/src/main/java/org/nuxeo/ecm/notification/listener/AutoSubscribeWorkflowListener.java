/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.listener;

import static java.util.Collections.singletonMap;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.notification.resolver.WorkflowUpdatesResolver.RESOLVER_NAME;
import static org.nuxeo.ecm.notification.resolver.WorkflowUpdatesResolver.WORKFLOW_ID_KEY;
import static org.nuxeo.ecm.platform.routing.core.audit.RoutingAuditHelper.WORKFLOW_INITATIOR;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener automatically subscribing the initiator of a Workflow to its updates.
 *
 * @since XXX
 * @XXX Move the listener in nuxeo-routing-core when the notifications will be merged in the platform.
 */
public class AutoSubscribeWorkflowListener implements EventListener {

    @Override
    public void handleEvent(Event event) {
        // Get the workflow initiator
        String initiator = (String) event.getContext().getProperty(WORKFLOW_INITATIOR);
        if (initiator == null || initiator.equals(SYSTEM_USERNAME)) {
            return;
        }

        // Subscribe the initiator to the resolver for workflow updates
        subscribeUserToWorkflow(event, initiator, RESOLVER_NAME);
    }

    /**
     * Subscribe the given user to the Workflow document defined in the event context.
     */
    public void subscribeUserToWorkflow(Event event, String user, String resolverName) {
        if (!(event.getContext() instanceof DocumentEventContext)) {
            throw new NuxeoException(String.format("The event %s is not a Document event.", event.getName()));
        }
        DocumentModel route = ((DocumentEventContext) event.getContext()).getSourceDocument();

        // Subscribe the originating user of the creation to the document
        NotificationService ns = Framework.getService(NotificationService.class);
        ns.subscribe(user, resolverName, singletonMap(WORKFLOW_ID_KEY, route.getId()));
    }
}
