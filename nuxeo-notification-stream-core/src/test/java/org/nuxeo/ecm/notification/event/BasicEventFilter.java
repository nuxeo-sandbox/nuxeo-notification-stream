/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.event;

import static org.nuxeo.ecm.notification.transformer.EventFilteringTransformer.PROP_FLAG_FILTER;

import java.util.Map;

import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Basic filter that filters events only if there is a flag in the context.
 *
 * @since 0.1
 */
public class BasicEventFilter extends EventFilter {
    @Override
    public boolean acceptEvent(Map<String, EventRecord> events, EventRecord event) {
        if (event.getContext().get(PROP_FLAG_FILTER) != null
                && event.getContext().get(PROP_FLAG_FILTER).equals("true")) {
            return false;
        }
        return true;
    }
}
