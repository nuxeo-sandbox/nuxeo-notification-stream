/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.transformer;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.event.Event;

/**
 * Another basic EventTransformer adding a few information in the context for documentModified events.
 *
 * @since 0.1
 */
public class AnotherBasicTransformer extends EventTransformer {

    public static final String KEY_INFO = "info";

    @Override
    public boolean accept(Event event) {
        return event.getName().equals(DOCUMENT_UPDATED);
    }

    @Override
    public Map<String, String> buildEventRecordContext(Event event) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put(KEY_INFO, (String) event.getContext().getProperty(KEY_INFO));
        return ctx;
    }
}
