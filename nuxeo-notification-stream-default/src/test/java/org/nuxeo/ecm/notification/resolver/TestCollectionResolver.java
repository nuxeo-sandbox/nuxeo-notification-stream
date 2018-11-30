/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.collections.api.CollectionConstants.ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.BEFORE_ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.BEFORE_REMOVED_FROM_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.REMOVED_FROM_COLLECTION;
import static org.nuxeo.ecm.notification.resolver.CollectionResolver.COLLECTION_DOC_ID;
import static org.nuxeo.ecm.notification.transformer.CollectionEventTransformer.PROP_COLLECTION_REF;
import static org.nuxeo.ecm.notification.transformer.CollectionEventTransformer.PROP_TYPE_REF;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Test class for the resolver {@link CollectionResolver}.
 *
 * @since XXX
 */
public class TestCollectionResolver {

    @Test
    public void resolverOnlyAcceptsAddedAndRemovedEvents() {
        Resolver resolver = new CollectionResolver().withId("collection");
        EventRecord.EventRecordBuilder builder = EventRecord.builder().withEventName(ADDED_TO_COLLECTION);
        assertThat(resolver.accept(builder.build())).isTrue();
        assertThat(resolver.accept(builder.withEventName(REMOVED_FROM_COLLECTION).build())).isTrue();
        assertThat(resolver.accept(builder.withEventName(BEFORE_ADDED_TO_COLLECTION).build())).isFalse();
        assertThat(resolver.accept(builder.withEventName(BEFORE_REMOVED_FROM_COLLECTION).build())).isFalse();
    }

    @Test
    public void resolverComputesProperKeyForSubscriptions() {
        Map<String, String> ctx = new HashMap<>();
        ctx.put(PROP_COLLECTION_REF, "0000-1111");
        ctx.put(PROP_TYPE_REF, "1");
        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                                                            .withEventName(ADDED_TO_COLLECTION)
                                                            .withContext(ctx);
        CollectionResolver resolver = (CollectionResolver) new CollectionResolver().withId("collection");
        assertThat(resolver.computeSubscriptionsKey(resolver.buildNotifierContext(builder.build()))).isEqualTo(
                "collection:0000-1111");
    }

    @Test
    public void resolverCreatesContextForNotifier() {
        Map<String, String> ctx = new HashMap<>();
        ctx.put(PROP_COLLECTION_REF, "0000-1111");
        ctx.put(PROP_TYPE_REF, "1");
        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                                                            .withEventName(ADDED_TO_COLLECTION)
                                                            .withContext(ctx);
        Resolver resolver = new CollectionResolver().withId("collection");
        Map<String, String> ctxNotifier = resolver.buildNotifierContext(builder.build());
        assertThat(ctxNotifier).hasSize(1);
        assertThat(ctxNotifier.get(COLLECTION_DOC_ID)).isEqualTo("0000-1111");
    }
}
