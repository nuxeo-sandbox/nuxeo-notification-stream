/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Nuxeo
 */

package org.nuxeo.ecm.notification.entities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.platform.url.DocumentViewImpl;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolve every entities from a Notification object; fetch Document from session, get user's metadata, ...
 *
 * @since XXX
 */
public class TextEntitiesSupplier {
    private static final Logger log = LogManager.getLogger(TextEntitiesSupplier.class);

    protected final Notification notif;

    protected CoreSession session;

    protected TextEntitiesSupplier(Notification notif) {
        this.notif = notif;
    }

    public void setSession(CoreSession session) {
        this.session = session;
    }

    public static void resolve(Notification notif) {
        new TextEntitiesSupplier(notif).execute();
    }

    protected void execute() {
        if (!requiresSession()) {
            resolveEntities();
        } else {
            TransactionHelper.runInTransaction(() -> {
                try {
                    LoginContext loginContext = Framework.loginAsUser(notif.getOriginatingUser());
                    String repository = notif.getSourceRepository();
                    try (CloseableCoreSession session = CoreInstance.openCoreSession(repository)) {
                        setSession(session);

                        resolveEntities();
                    } finally {
                        setSession(null);
                        loginContext.logout();
                    }
                } catch (LoginException e) {
                    throw new NuxeoException(e);
                }
            });
        }
    }

    protected boolean requiresSession() {
        return notif.entities.stream().map(TextEntity::getType).anyMatch(TextEntity.DOCUMENT::equals);
    }

    protected void resolveEntities() {
        notif.entities.forEach(e -> e.values = resolve(e));
    }

    public Map<String, String> resolve(TextEntity textEntity) {
        switch (textEntity.getType()) {
            case TextEntity.DOCUMENT:
                return resolveDocument(textEntity);
            case TextEntity.USERNAME:
                return resolverUsername(textEntity);
        }
        return Collections.emptyMap();
    }

    protected Map<String, String> resolveDocument(TextEntity textEntity) {
        DocumentRef docRef = textEntity.id.startsWith("/") ? new PathRef(textEntity.id) : new IdRef(textEntity.id);
        if (!session.exists(docRef)) {
            return Collections.emptyMap();
        }

        DocumentModel doc = session.getDocument(docRef);

        Map<String, String> values = new HashMap<>();
        values.put("id", doc.getId());
        values.put("path", doc.getPathAsString());
        values.put("name", doc.getName());
        values.put("repository", doc.getRepositoryName());
        values.put("url", getUrl(doc));
        values.put("title", doc.getTitle());
        return values;
    }

    protected String getUrl(DocumentModel doc) {
        DocumentViewCodecManager viewCodecManager = Framework.getService(DocumentViewCodecManager.class);
        return viewCodecManager.getUrlFromDocumentView(new DocumentViewImpl(doc), true,
                Framework.getProperty("nuxeo.url"));
    }

    protected Map<String, String> resolverUsername(TextEntity textEntity) {
        UserManager userManager = Framework.getService(UserManager.class);
        if (userManager == null) {
            log.error("Unable to find UserManager Service.");
            return Collections.emptyMap();
        }

        NuxeoPrincipal principal = userManager.getPrincipal(textEntity.getId());
        if (principal == null) {
            return Collections.emptyMap();
        }

        Map<String, String> values = new HashMap<>();
        values.put("username", principal.getName());
        values.put("firstName", principal.getFirstName());
        values.put("firstLame", principal.getLastName());
        values.put("email", principal.getEmail());
        return values;
    }
}
