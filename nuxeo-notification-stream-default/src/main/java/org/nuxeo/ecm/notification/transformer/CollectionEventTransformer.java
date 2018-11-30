/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.transformer;

import static org.nuxeo.ecm.collections.api.CollectionConstants.ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_REF_EVENT_CTX_PROP;
import static org.nuxeo.ecm.collections.api.CollectionConstants.REMOVED_FROM_COLLECTION;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.InstanceRef;
import org.nuxeo.ecm.core.event.Event;

/**
 * EventTransformer for the events related to documents added or removed from a Collection.
 *
 * @since XXX
 */
public class CollectionEventTransformer extends EventTransformer {

    public static final String PROP_COLLECTION_REF = "collectionRef";

    public static final String PROP_TYPE_REF = "typeRef";

    @Override
    public boolean accept(Event event) {
        return event.getName().equals(ADDED_TO_COLLECTION) || event.getName().equals(REMOVED_FROM_COLLECTION);
    }

    @Override
    public Map<String, String> buildEventRecordContext(Event event) {
        Map<String, String> ctx = new HashMap<>();
        // Extract the id of the collection
        DocumentRef collectionRef = (DocumentRef) event.getContext().getProperty(COLLECTION_REF_EVENT_CTX_PROP);
        if (collectionRef instanceof InstanceRef) {
            ctx.put(PROP_COLLECTION_REF, ((DocumentModel) collectionRef.reference()).getId());
        } else {
            ctx.put(PROP_COLLECTION_REF, collectionRef.toString());
        }
        ctx.put(PROP_TYPE_REF, String.valueOf(collectionRef.type()));
        return ctx;
    }
}
