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
 * Basic EventTransformer for test purpose.
 *
 * @since 0.1
 */
public class BasicTransformer extends EventTransformer {

    public static final String KEY_EVENT_NAME = "eventName";

    @Override
    public boolean accept(Event event) {
        return true;
    }

    @Override
    public Map<String, String> buildEventRecordContext(Event event) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put(KEY_EVENT_NAME, event.getName());
        ctx.put(id, "Transformer Name");
        return ctx;
    }
}
