/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.collections.api.CollectionConstants.ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.notification.event.CollectionEventsFilter;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Test the filter of collection events.
 *
 * @since 0.1
 */
public class TestCollectionEventsFilter {

    @Test
    public void testFilterEvents() {
        CollectionEventsFilter filter = new CollectionEventsFilter();

        // Test filter without collection events
        Map<String, EventRecord> listEvents = new HashMap<>();
        EventRecord event1 = EventRecord.builder().withDocumentId("0000-1111").withEventName(DOCUMENT_CREATED).build();
        EventRecord event2 = EventRecord.builder().withDocumentId("0000-1111").withEventName(BEFORE_DOC_UPDATE).build();
        EventRecord event3 = EventRecord.builder().withDocumentId("0000-1111").withEventName(DOCUMENT_UPDATED).build();
        EventRecord event4 = EventRecord.builder().withDocumentId("0000-1111").withEventName(DOCUMENT_REMOVED).build();

        listEvents.put("1", event1);
        listEvents.put("2", event2);
        listEvents.put("3", event3);
        listEvents.put("4", event4);
        assertThat(filter.acceptEvent(listEvents, listEvents.get("3"))).isTrue();

        // Add Collection events on the same document
        EventRecord event5 = EventRecord.builder().withDocumentId("0000-1111").withEventName(ADDED_TO_COLLECTION).build();
        listEvents.put("5", event5);
        assertThat(filter.acceptEvent(listEvents, listEvents.get("3"))).isFalse();

        // Update the collection event to use a different document source id
        event5 = EventRecord.builder().withDocumentId("0000-2222").withEventName(ADDED_TO_COLLECTION).build();
        listEvents.put("5", event5);
        assertThat(filter.acceptEvent(listEvents, listEvents.get("3"))).isTrue();
    }
}
