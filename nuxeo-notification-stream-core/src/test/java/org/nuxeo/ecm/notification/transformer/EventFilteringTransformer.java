/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.transformer;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.event.Event;

/**
 * Transformer that adds a flag in the context of the EventRecord to test the filtering feature in the
 * EventStreamListener.
 * 
 * @since XXX
 */
public class EventFilteringTransformer extends EventTransformer {

    public static final String PROP_FLAG_FILTER = "filterEvent";

    @Override
    public boolean accept(Event event) {
        return true;
    }

    @Override
    public Map<String, String> buildEventRecordContext(Event event) {
        Map<String, String> ctx = new HashMap<>();
        if (event.getContext().getProperty(PROP_FLAG_FILTER) != null) {
            ctx.put(PROP_FLAG_FILTER, String.valueOf(event.getContext().getProperty(PROP_FLAG_FILTER)));
        } else {
            ctx.put(PROP_FLAG_FILTER, "false");
        }
        return ctx;
    }
}
