/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.listener;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.DOC_ID_KEY;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.RESOLVER_NAME;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener automatically subscribing the creator of a Document to its updates. It does not subscribe admins users.
 *
 * @since XXX
 */
public class AutoSubscribeAtCreationListener implements PostCommitEventListener {

    @Override
    public void handleEvent(EventBundle eventBundle) {
        eventBundle.forEach(this::handleEvent);
    }

    public void handleEvent(Event event) {
        // If the document is created as system, do not register the user.
        // Same if the user is part of the group Administrators
        NuxeoPrincipal originatingUser = event.getContext().getPrincipal();
        if (originatingUser == null || originatingUser.equals(SYSTEM_USERNAME) || originatingUser.isAdministrator()) {
            return;
        }

        if (!(event.getContext() instanceof DocumentEventContext)) {
            throw new NuxeoException(String.format("The event %s is not a Document event.", event.getName()));
        }
        DocumentModel source = ((DocumentEventContext) event.getContext()).getSourceDocument();

        // Subscribe the originating user of the creation to the document
        Map<String, String> ctx = new HashMap<>();
        ctx.put(DOC_ID_KEY, source.getId());
        NotificationService ns = Framework.getService(NotificationService.class);
        ns.subscribe(originatingUser.getName(), RESOLVER_NAME, ctx);
    }
}
