/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Resolver to notify users whenever a followed document is updated.
 *
 * @since XXX
 */
public class DocumentUpdateResolver extends SubscribableResolver {

    public static final String RESOLVER_NAME = "documentUpdated";

    public static final String DOC_ID_KEY = "docId";

    public static final String EVENT_KEY = "event";

    @Override
    public List<String> getRequiredContextFields() {
        return Arrays.asList(DOC_ID_KEY, EVENT_KEY);
    }

    @Override
    public boolean accept(EventRecord eventRecord) {
        return eventRecord.getEventName().equals(DOCUMENT_UPDATED);
    }

    @Override
    public Map<String, String> buildNotifierContext(EventRecord eventRecord) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put(EVENT_KEY, eventRecord.getEventName());
        ctx.put(DOC_ID_KEY, eventRecord.getDocumentSourceId());
        return ctx;
    }
}
