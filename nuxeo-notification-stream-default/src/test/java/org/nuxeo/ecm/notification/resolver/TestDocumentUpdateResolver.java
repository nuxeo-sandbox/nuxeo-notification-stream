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
import static org.nuxeo.ecm.notification.resolver.DocumentUpdateResolver.RESOLVER_NAME;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;

import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Test class for the Resolver when a document is updated.
 *
 * @since 0.1
 */
public class TestDocumentUpdateResolver {

    @Test
    public void resolverOnlyAcceptsDocumentModifiedEvents() {
        // Test resolver for regular documents
        DocumentUpdateResolver resolver = (DocumentUpdateResolver) new DocumentUpdateResolver().withId(RESOLVER_NAME);
        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                                                            .withDocumentId("0000-1111")
                                                            .withEventName(DOCUMENT_UPDATED)
                                                            .withDocumentType("File");
        assertThat(resolver.accept(builder.build())).isTrue();
        assertThat(resolver.accept(builder.withEventName(DOCUMENT_CREATED).build())).isFalse();
        assertThat(resolver.accept(builder.withEventName(DOCUMENT_REMOVED).build())).isFalse();
        assertThat(resolver.accept(builder.withEventName(BEFORE_DOC_UPDATE).build())).isFalse();

        // Test resolver for comments
        builder.withDocumentType(COMMENT_DOC_TYPE).withEventName(DOCUMENT_CREATED);
        assertThat(resolver.accept(builder.build())).isTrue();
        assertThat(resolver.accept(builder.withEventName(DOCUMENT_REMOVED).build())).isFalse();
        assertThat(resolver.accept(builder.withEventName(BEFORE_DOC_UPDATE).build())).isFalse();
    }

    @Test
    public void resolverComputesProperKeyForSubscriptions() {
        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                                                            .withDocumentId("0000-1111")
                                                            .withEventName(DOCUMENT_UPDATED);
        DocumentUpdateResolver resolver = (DocumentUpdateResolver) new DocumentUpdateResolver().withId(RESOLVER_NAME);
        assertThat(resolver.computeSubscriptionsKey(builder.build().getContext())).isEqualTo(
                RESOLVER_NAME + ":0000-1111");
    }

    @Test
    public void resolverCreatesContextForNotifier() {
        EventRecord.EventRecordBuilder builder = EventRecord.builder()
                                                            .withDocumentId("0000-1111")
                                                            .withEventName(DOCUMENT_UPDATED);
        DocumentUpdateResolver resolver = (DocumentUpdateResolver) new DocumentUpdateResolver().withId(RESOLVER_NAME);
        Map<String, String> ctx = resolver.buildNotifierContext(null, builder.build());
        assertThat(ctx).isEmpty();
    }
}
