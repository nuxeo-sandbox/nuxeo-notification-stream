/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.event;

import static org.nuxeo.ecm.collections.api.CollectionConstants.ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.REMOVED_FROM_COLLECTION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Filter duplicated events related to Collection in the same transaction. The biggest issue is when adding a document
 * to a Collection. There is a specific event triggered for that but also a documentModified event on the document added
 * that will trigger a second notification. The filter checks if there is an event 'addedToCollection' and then checks
 * the presence of a 'DocumentModified' event on the document added.
 *
 * @since 0.1
 */
public class CollectionEventsFilter extends EventFilter {

    @Override
    public boolean acceptEvent(Map<String, EventRecord> events, EventRecord eventRecord) {
        // Only filter if it's a document modified event and if there is also a collection event on the same doc
        if (!eventRecord.getEventName().equals(DOCUMENT_UPDATED)
                && !eventRecord.getEventName().equals(BEFORE_DOC_UPDATE)) {
            return true;
        }

        // Check if there are any collection events for the source document id
        return events.entrySet()
                     .stream()
                     .filter(x -> isUpdatedCollectionContentEvent(x.getValue()) && StringUtils.equals(
                             eventRecord.getDocumentSourceId(), x.getValue().getDocumentSourceId()))
                     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                     .isEmpty();
    }

    protected boolean isUpdatedCollectionContentEvent(EventRecord eventRecord) {
        return eventRecord.getEventName().equals(ADDED_TO_COLLECTION)
                || eventRecord.getEventName().equals(REMOVED_FROM_COLLECTION);
    }
}
