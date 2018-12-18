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

package org.nuxeo.ecm.notification.resolver;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Resolver class aims to be able to transform a CoreEvent to a Notification object
 *
 * @since XXX
 */
public abstract class Resolver {

    protected String id;

    public String getId() {
        return id == null ? this.getClass().getSimpleName() : id;
    }

    protected Resolver withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Compute Resolver message property key following the pattern: label.notification.resolver.RESOLVER_NAME
     * 
     * @return resolver's label message property entry key
     */
    public String getLabelKey() {
        return String.format("label.notification.resolver.%s", getId());
    }

    /**
     * Compute Resolver message property key following the pattern: description.notification.resolver.RESOLVER_NAME
     * 
     * @return resolver's description message property entry key
     */
    public String getDescriptionKey() {
        return String.format("description.notification.resolver.%s", getId());
    }

    /**
     * Return configured notification's message key to pass to the notifier
     *
     * @return message to display as a notification, return an empty string if empty.
     */
    public String getMessageKey() {
        return String.format("message.notification.resolver.%s", getId());
    }

    /**
     * Check if the resolver is able to compute the current Event in a Notification Object.
     * <p>
     * The method is executed on EVERY core event, his resolution MUST be as fast as possible.
     *
     * @param eventRecord that could be transformed.
     * @return true if it assignable, false otherwise.
     */
    public abstract boolean accept(EventRecord eventRecord);

    /**
     * Resolve target users that have to be notified
     * <p>
     * The method is executed on EVERY corresponding notification, his resolution MUST be as fast as possible.
     *
     * @param eventRecord from the context that contain resolution needed
     * @return list of target users, of an empty list otherwise.
     */
    public abstract Stream<String> resolveTargetUsers(EventRecord eventRecord);

    /**
     * Return some contextual informations needed to passed to the Notifier
     * 
     * @param targetUsername user that will receive the notification
     * @param eventRecord that generates the notification
     * @return a map with contextual informations like document title, document path, document url, ...
     */
    public abstract Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord);

    protected static <T> T withDocument(EventRecord eventRecord, Function<DocumentModel, T> func) {
        return withSession(eventRecord, session -> func.apply(session.getDocument(eventRecord.getDocumentSourceRef())));
    }

    protected static <T> T withSession(EventRecord eventRecord, Function<CoreSession, T> func) {
        AtomicReference<T> ret = new AtomicReference<>();
        TransactionHelper.runInTransaction(() -> {
            try {
                LoginContext loginContext = Framework.loginAsUser(eventRecord.getUsername());
                String repository = eventRecord.getRepository();
                try (CloseableCoreSession session = CoreInstance.openCoreSession(repository)) {
                    ret.set(func.apply(session));
                } finally {
                    loginContext.logout();
                }
            } catch (LoginException e) {
                throw new NuxeoException(e);
            }
        });

        return ret.get();
    }
}
