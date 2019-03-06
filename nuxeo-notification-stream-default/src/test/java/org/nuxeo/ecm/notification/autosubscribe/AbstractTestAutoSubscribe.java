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

import javax.inject.Inject;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.model.Subscribers;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 0.1
 */
public class AbstractTestAutoSubscribe {

    @Inject
    protected CoreSession session;

    @Inject
    protected UserManager userManager;

    @Inject
    protected NotificationService ns;

    @Inject
    protected TransactionalFeature txFeature;

    protected void createUserAndAddPermissionsToCreateDocuments(String username) {
        // Create a new user
        DocumentModel newUser = userManager.getBareUserModel();
        newUser.setPropertyValue(userManager.getUserIdField(), username);
        userManager.createUser(newUser);

        // Add permission to the user to create a new document
        PathRef rootRef = new PathRef("/");
        ACP acp = session.getACP(rootRef);
        ACL existingACL = acp.getOrCreateACL();
        existingACL.add(new ACE(username, EVERYTHING, true));
        session.setACP(new PathRef("/"), acp, true);
    }

    protected void checkSubscription(Map<String, String> ctx, String resolverName, String... users) {
        Subscribers subscribers = ns.getSubscriptions(resolverName, ctx);
        assertThat(subscribers.getUsernames()).hasSize(users.length);
        assertThat(subscribers.getUsernames()).containsOnly(users);
    }
}
