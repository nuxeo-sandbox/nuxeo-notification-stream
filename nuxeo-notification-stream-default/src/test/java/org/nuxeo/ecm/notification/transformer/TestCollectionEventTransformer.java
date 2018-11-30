/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.collections.api.CollectionConstants.ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.BEFORE_ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.BEFORE_REMOVED_FROM_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_REF_EVENT_CTX_PROP;
import static org.nuxeo.ecm.collections.api.CollectionConstants.REMOVED_FROM_COLLECTION;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.notification.transformer.CollectionEventTransformer.PROP_COLLECTION_REF;
import static org.nuxeo.ecm.notification.transformer.CollectionEventTransformer.PROP_TYPE_REF;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.collections.api.CollectionConstants;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;

/**
 * Test for the EventTransformer {@link CollectionEventTransformer} on Collection events.
 *
 * @since XXX
 */
public class TestCollectionEventTransformer {

    @Test
    public void testTransformerAccept() {
        CollectionEventTransformer transformer = new CollectionEventTransformer();
        assertThat(transformer.accept(new EventImpl(DOCUMENT_CREATED, null))).isFalse();
        assertThat(transformer.accept(new EventImpl(BEFORE_ADDED_TO_COLLECTION, null))).isFalse();
        assertThat(transformer.accept(new EventImpl(BEFORE_REMOVED_FROM_COLLECTION, null))).isFalse();
        assertThat(transformer.accept(new EventImpl(ADDED_TO_COLLECTION, null))).isTrue();
        assertThat(transformer.accept(new EventImpl(REMOVED_FROM_COLLECTION, null))).isTrue();
    }

    @Test
    public void testTransformerBuildContext() {
        CollectionEventTransformer transformer = new CollectionEventTransformer();

        // Test the context builder with an IdRef of a Collection
        Map<String, Serializable> props = new HashMap<>();
        props.put(COLLECTION_REF_EVENT_CTX_PROP, new IdRef("0000-1111-2222"));
        EventContext eventCtx = new EventContextImpl();
        eventCtx.setProperties(props);
        // Check the built context
        Map<String, String> ctx = transformer.buildEventRecordContext(new EventImpl(ADDED_TO_COLLECTION, eventCtx));
        assertThat(ctx).hasSize(2);
        assertThat(ctx).containsKeys(PROP_COLLECTION_REF, PROP_TYPE_REF);
        assertThat(ctx.get(PROP_COLLECTION_REF)).isEqualTo("0000-1111-2222");
        assertThat(ctx.get(PROP_TYPE_REF)).isEqualTo(String.valueOf(DocumentRef.ID));

        // Test the context builder with an PathRef of a Collection
        props.put(COLLECTION_REF_EVENT_CTX_PROP, new PathRef("/default/test/document"));
        eventCtx = new EventContextImpl();
        eventCtx.setProperties(props);
        // Check the built context
        ctx = transformer.buildEventRecordContext(new EventImpl(ADDED_TO_COLLECTION, eventCtx));
        assertThat(ctx).hasSize(2);
        assertThat(ctx).containsKeys(PROP_COLLECTION_REF, PROP_TYPE_REF);
        assertThat(ctx.get(PROP_COLLECTION_REF)).isEqualTo("/default/test/document");
        assertThat(ctx.get(PROP_TYPE_REF)).isEqualTo(String.valueOf(DocumentRef.PATH));
    }
}
