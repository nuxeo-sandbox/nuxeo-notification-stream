/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import static org.nuxeo.ecm.collections.api.CollectionConstants.ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.REMOVED_FROM_COLLECTION;
import static org.nuxeo.ecm.notification.transformer.CollectionEventTransformer.PROP_COLLECTION_REF;
import static org.nuxeo.ecm.notification.transformer.CollectionEventTransformer.PROP_TYPE_REF;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Resolver to notify a user when the content of a Collection is updated.
 *
 * @since XXX
 */
public class CollectionUpdatesResolver extends AbstractCollectionResolver {

    @Override
    public boolean accept(EventRecord eventRecord) {
        // Accept the event if it's an add/remove event
        return eventRecord.getEventName().equals(ADDED_TO_COLLECTION)
                || eventRecord.getEventName().equals(REMOVED_FROM_COLLECTION);
    }

    @Override
    public Map<String, String> buildNotifierContext(EventRecord eventRecord) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put(COLLECTION_DOC_ID, getCollectionId(eventRecord));
        return ctx;
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        // The collection id is fetched and the target users are resolved using this collection id.
        Set<String> targetUsers = new HashSet<>();
        // Get the collection Id
        Map<String, String> ctx = new HashMap<>();
        ctx.put(COLLECTION_DOC_ID, getCollectionId(eventRecord));
        getSubscriptions(ctx).getUsernames().forEach(targetUsers::add);

        return targetUsers.stream();
    }

    /**
     * Get the collection id from the collection reference given in the EventRecord.
     *
     * @param eventRecord The EventRecord to help determine the collection id.
     * @return The document model uuid of the Collection.
     */
    protected String getCollectionId(EventRecord eventRecord) {
        String ref = eventRecord.getContext().get(PROP_COLLECTION_REF);
        String type = eventRecord.getContext().get(PROP_TYPE_REF);

        // Throw an exception if the collection reference is missing from the event record
        if (StringUtils.isBlank(ref) || StringUtils.isBlank(type)) {
            throw new NuxeoException("Missing collection ref in EventRecord");
        }

        if (type.equals(String.valueOf(DocumentRef.PATH))) {
            // The collection must be fetched to get the document id
            AtomicReference<String> ret = new AtomicReference<>();
            TransactionHelper.runInTransaction(() -> {
                try {
                    LoginContext loginContext = Framework.loginAsUser(eventRecord.getUsername());
                    String repository = eventRecord.getRepository();
                    try (CloseableCoreSession session = CoreInstance.openCoreSession(repository)) {
                        ret.set(session.getDocument(new PathRef(ref)).getId());
                    } finally {
                        loginContext.logout();
                    }
                } catch (LoginException e) {
                    throw new NuxeoException(e);
                }
            });
            return ret.get();
        } else {
            // The collection id is already the collection Ref
            return ref;
        }
    }
}
