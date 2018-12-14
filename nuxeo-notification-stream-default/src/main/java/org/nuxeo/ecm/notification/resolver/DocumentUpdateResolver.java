/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_DOC_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ANCESTOR_IDS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Resolver to notify users whenever a followed document is updated.
 *
 * @since XXX
 */
public class DocumentUpdateResolver extends SubscribableResolver {

    public static final String RESOLVER_NAME = "documentUpdated";

    public static final String COMMENT_ID_KEY = "commentIdKey";

    public static final String COMMENT_AUTHOR_KEY = "commentAuthorKey";

    @Override
    public List<String> getRequiredContextFields() {
        return Arrays.asList(SOURCE_DOC_ID);
    }

    @Override
    public boolean accept(EventRecord eventRecord) {
        // The event record is processed if the document has been updated or if a new comment has been created on the
        // document
        return eventRecord.getEventName().equals(DOCUMENT_UPDATED)
                || (eventRecord.getEventName().equals(DOCUMENT_CREATED)
                        && COMMENT_DOC_TYPE.equals(eventRecord.getDocumentSourceType()));
    }

    @Override
    public Map<String, String> buildNotifierContext(EventRecord eventRecord) {
        Map<String, String> ctx = new HashMap<>();
        // Handle differently if the document is a comment or if it's another type of document
        if (COMMENT_DOC_TYPE.equals(eventRecord.getDocumentSourceType())) {
            // Fetch the document attached to the comment
            // Put the id of the document attached to the comment for the document id key and put the id of the comment
            // in a different key
            ctx.put(SOURCE_DOC_ID, getAncestorId(eventRecord));
            ctx.put(COMMENT_ID_KEY, eventRecord.getDocumentSourceId());
            ctx.put(COMMENT_AUTHOR_KEY, eventRecord.getUsername());
        }
        return ctx;
    }

    /**
     * Override the default resolveTargetUser method to handle the creation of comment on a document. Instead of using
     * the source doc id (which is the comment id), the ancestor is fetched and used as source doc id.
     * 
     * @param eventRecord
     * @return
     */
    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        if (COMMENT_DOC_TYPE.equals(eventRecord.getDocumentSourceType())) {
            Map<String, String> ctx = new HashMap<>();
            // Get the ancestor id and fetch the subscribers for this document
            ctx.put(SOURCE_DOC_ID, getAncestorId(eventRecord));
            Set<String> targetUsers = new HashSet<>();
            getSubscriptions(ctx).getUsernames().forEach(targetUsers::add);
            return targetUsers.stream();
        } else {
            return super.resolveTargetUsers(eventRecord);
        }
    }

    protected String getAncestorId(EventRecord eventRecord) {
        return withDocument(eventRecord, d -> {
            List<String> ancestors = ((List<String>) d.getPropertyValue(
                    COMMENT_ANCESTOR_IDS)).stream()
                                          .filter(id -> d.getCoreSession()
                                                         .getDocument(new IdRef(id))
                                                         .hasFacet("Commentable"))
                                          .collect(Collectors.toList());
            if (ancestors.size() > 0) {
                return ancestors.get(0);
            }
            return null;
        });
    }
}
