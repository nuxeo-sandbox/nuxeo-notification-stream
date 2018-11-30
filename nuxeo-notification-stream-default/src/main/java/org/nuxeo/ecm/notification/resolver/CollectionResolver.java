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

import static org.nuxeo.ecm.collections.api.CollectionConstants.ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_MEMBER_SCHEMA_NAME;
import static org.nuxeo.ecm.collections.api.CollectionConstants.REMOVED_FROM_COLLECTION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.notification.transformer.CollectionEventTransformer.PROP_COLLECTION_REF;
import static org.nuxeo.ecm.notification.transformer.CollectionEventTransformer.PROP_TYPE_REF;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Resolver to be able to be notified when subscribe to a collection's document modifications and when a document is
 * added or removed from the collection.
 *
 * @since XXX
 */
public class CollectionResolver extends SubscribableResolver {

    public static final String COLLECTION_DOC_ID = "collectionDocId";

    @Override
    public boolean accept(EventRecord eventRecord) {
        // Accept the event if it's an add/remove event or if it's documentModified event on a Collection member
        return isUpdatedCollectionContentEvent(eventRecord) || eventRecord.getEventName().equals(DOCUMENT_UPDATED)
                && withDocument(eventRecord, doc -> doc.hasSchema(COLLECTION_MEMBER_SCHEMA_NAME));
    }

    @Override
    public List<String> getRequiredContextFields() {
        return Arrays.asList(COLLECTION_DOC_ID);
    }

    @Override
    public Map<String, String> buildNotifierContext(EventRecord eventRecord) {
        if (isUpdatedCollectionContentEvent(eventRecord)) {
            Map<String, String> ctx = new HashMap<>();
            ctx.put(COLLECTION_DOC_ID, getCollectionId(eventRecord));
            return ctx;
        }
        return Collections.emptyMap();
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        Set<String> targetUsers = new HashSet<>();

        // If the event is an update of the content of the collection, the collection id is fetched and the target users
        // are resolved using this collection id.
        if (isUpdatedCollectionContentEvent(eventRecord)) {
            // Get the collection Id
            Map<String, String> ctx = new HashMap<>();
            ctx.put(COLLECTION_DOC_ID, getCollectionId(eventRecord));
            getSubscriptions(ctx).getUsernames().forEach(targetUsers::add);
        } else {
            // The collections of the updated document must be fetched and the target users are resolved for each
            // collection where the document is added
            withDocument(eventRecord, doc -> doc.getAdapter(CollectionMember.class).getCollectionIds()).forEach(
                    collectionId -> {
                        Map<String, String> ctx = new HashMap<>();
                        ctx.put(COLLECTION_DOC_ID, collectionId);

                        getSubscriptions(ctx).getUsernames().forEach(targetUsers::add);
                    });
        }

        return targetUsers.stream();
    }

    protected boolean isUpdatedCollectionContentEvent(EventRecord eventRecord) {
        return eventRecord.getEventName().equals(ADDED_TO_COLLECTION)
                || eventRecord.getEventName().equals(REMOVED_FROM_COLLECTION);
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
