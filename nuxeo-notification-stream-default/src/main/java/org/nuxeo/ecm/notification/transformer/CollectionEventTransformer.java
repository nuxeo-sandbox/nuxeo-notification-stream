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
import static org.nuxeo.ecm.notification.resolver.AbstractCollectionResolver.COLLECTION_DOC_ID;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.InstanceRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.Event;

/**
 * EventTransformer for the events related to documents added or removed from a Collection.
 *
 * @since 0.1
 */
public class CollectionEventTransformer extends EventTransformer {

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
            ctx.put(COLLECTION_DOC_ID, ((DocumentModel) collectionRef.reference()).getId());
        } else if (collectionRef instanceof PathRef) {
            // Fetch the document to get the id
            DocumentModel collection = event.getContext().getCoreSession().getDocument(collectionRef);
            ctx.put(COLLECTION_DOC_ID, collection.getId());
        }else {
            // The ref is an IdRef
            ctx.put(COLLECTION_DOC_ID, collectionRef.toString());
        }
        return ctx;
    }
}
