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
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.BEFORE_DOC_UPDATE;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.DOC_ID_KEY;
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.EVENT_KEY;

import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Test class for the Resolver when a document is updated.
 *
 * @since XXX
 */
public class TestDocumentUpdateResolver {

    @Test
    public void resolverOnlyAcceptsDocumentModifiedEvents() {
        DocumentUpdateResolver resolver = new DocumentUpdateResolver();
        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                                                            .withDocumentId("0000-1111")
                                                            .withEventName(DOCUMENT_UPDATED);
        assertThat(resolver.accept(builder.build())).isTrue();
        assertThat(resolver.accept(builder.withEventName(DOCUMENT_CREATED).build())).isFalse();
        assertThat(resolver.accept(builder.withEventName(DOCUMENT_REMOVED).build())).isFalse();
        assertThat(resolver.accept(builder.withEventName(BEFORE_DOC_UPDATE).build())).isFalse();
    }

    @Test
    public void resolverComputesProperKeyForSubscriptions() {
        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                                                            .withDocumentId("0000-1111")
                                                            .withEventName(DOCUMENT_UPDATED);
        DocumentUpdateResolver resolver = new DocumentUpdateResolver();
        assertThat(resolver.computeSubscriptionsKey(resolver.buildNotifierContext(builder.build()))).isEqualTo(
                "0000-1111:" + DOCUMENT_UPDATED);
    }

    @Test
    public void resolverCreatesContextForNotifier() {
        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                .withDocumentId("0000-1111")
                .withEventName(DOCUMENT_UPDATED);
        DocumentUpdateResolver resolver = new DocumentUpdateResolver();
        Map<String, String> ctx = resolver.buildNotifierContext(builder.build());
        assertThat(ctx).hasSize(2);
        assertThat(ctx).containsKeys(DOC_ID_KEY, EVENT_KEY);
        assertThat(ctx.get(DOC_ID_KEY)).isEqualTo("0000-1111");
        assertThat(ctx.get(EVENT_KEY)).isEqualTo(DOCUMENT_UPDATED);
    }
}
