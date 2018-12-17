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

import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_MEMBER_SCHEMA_NAME;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.nuxeo.ecm.collections.core.adapter.CollectionMember;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Resolver to be able to be notified when a document in a collection is updated.
 *
 * @since XXX
 */
public class CollectionResolver extends AbstractCollectionResolver {

    @Override
    public boolean accept(EventRecord eventRecord) {
        // Accept the event if it's documentModified event on a Collection member
        return eventRecord.getEventName().equals(DOCUMENT_UPDATED)
                && withDocument(eventRecord, doc -> doc.hasSchema(COLLECTION_MEMBER_SCHEMA_NAME));
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        return Collections.emptyMap();
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        Set<String> targetUsers = new HashSet<>();

        // The collections of the updated document must be fetched and the target users are resolved for each
        // collection where the document is added
        withDocument(eventRecord, doc -> doc.getAdapter(CollectionMember.class).getCollectionIds()).forEach(
                collectionId -> {
                    Map<String, String> ctx = new HashMap<>();
                    ctx.put(COLLECTION_DOC_ID, collectionId);

                    getSubscriptions(ctx).getUsernames().forEach(targetUsers::add);
                });

        return targetUsers.stream();
    }
}
