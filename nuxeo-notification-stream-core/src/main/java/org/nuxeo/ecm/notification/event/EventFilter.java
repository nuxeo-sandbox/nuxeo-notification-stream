/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.event;

import java.util.Map;

import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * The EventsStreamListener processes a bundle of events triggered in one transaction. Because there might be duplicated
 * events (for example, several updates on the same document), we don't want to send multiple notifications for those
 * duplicates. The following class allows to define a more complex logic to find and remove duplicates in a list of
 * events.
 *
 * @since XXX
 */
public abstract class EventFilter {

    protected String id;

    public String getId() {
        return id == null ? this.getClass().getSimpleName() : id;
    }

    protected EventFilter withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Return a filtered map of Events without the duplicates the instance knows how to remove.
     *
     * @param events Map of events to filter.
     * @return The filtered map.
     */
    public abstract boolean acceptEvent(Map<String, EventRecord> events, EventRecord event);
}
