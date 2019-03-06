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

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_DOC_ID;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.runtime.api.Framework;

/**
 * Allow users to be notified on updates through documents below a Folderish.
 *
 * @since 0.1
 */
public class DescendantChangesResolver extends SubscribableResolver {

    public static final String ANCESTOR_ID = "ancestorId";

    @Override
    public List<String> getRequiredContextFields() {
        return Arrays.asList(SOURCE_DOC_ID);
    }

    @Override
    public boolean accept(EventRecord eventRecord) {
        return eventRecord.getEventName().equals(DOCUMENT_UPDATED)
                || eventRecord.getEventName().equals(DOCUMENT_CREATED);
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        Set<String> sb = new HashSet<>();

        DocumentRef[] parent = withSession(eventRecord,
                session -> session.getParentDocumentRefs(eventRecord.getDocumentSourceRef()));

        Arrays.stream(parent).map(DocumentRef::toString).forEach(s -> {
            EventRecord parentRecord = EventRecord.builder().fromEvent(eventRecord).withDocumentId(s).build();
            super.resolveTargetUsers(parentRecord).forEach(sb::add);
        });

        return sb.stream();
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        DocumentRef[] parent = withSession(eventRecord,
                session -> session.getParentDocumentRefs(eventRecord.getDocumentSourceRef()));
        String ancestorId = Arrays.stream(parent)
                                  .map(DocumentRef::toString)
                                  .filter(s -> Framework.getService(NotificationService.class)
                                                        .hasSubscribe(targetUsername, getId(),
                                                                Collections.singletonMap(SOURCE_DOC_ID, s)))
                                  .findFirst()
                                  .orElse(null);

        if (ancestorId != null) {
            return Collections.singletonMap(ANCESTOR_ID, ancestorId);
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public void subscribe(String username, Map<String, String> ctx) {
        EventRecord tmpRecord = EventRecord.builder()
                                           .withUsername(username)
                                           .withDocumentId(ctx.get(SOURCE_DOC_ID))
                                           .build();
        boolean isFolder = withDocument(tmpRecord, DocumentModel::isFolder);
        if (!isFolder) {
            throw new NuxeoException("Unable to subscribe to this resolver with a non-folderish document: "
                    + tmpRecord.getDocumentSourceId());
        }

        super.subscribe(username, ctx);
    }
}
