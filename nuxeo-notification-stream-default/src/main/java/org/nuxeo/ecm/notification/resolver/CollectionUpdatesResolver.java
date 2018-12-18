/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import static org.nuxeo.ecm.collections.api.CollectionConstants.ADDED_TO_COLLECTION;
import static org.nuxeo.ecm.collections.api.CollectionConstants.REMOVED_FROM_COLLECTION;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Resolver to notify a user when the content of a Collection is updated.
 *
 * @since XXX
 */
public class CollectionUpdatesResolver extends AbstractCollectionResolver {

    public static final String CTX_ACTION = "action";

    public static final String CTX_ACTION_SUFFIX = "actionSuffix";

    @Override
    public boolean accept(EventRecord eventRecord) {
        // Accept the event if it's an add/remove event
        return eventRecord.getEventName().equals(ADDED_TO_COLLECTION)
                || eventRecord.getEventName().equals(REMOVED_FROM_COLLECTION);
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        Map<String, String> ctx = new HashMap<>();
        // Add the context variables for the text of the notification messageKey
        if (eventRecord.getEventName().equals(ADDED_TO_COLLECTION)) {
            ctx.put(CTX_ACTION, "added");
            ctx.put(CTX_ACTION_SUFFIX, "to");
        } else {
            ctx.put(CTX_ACTION, "removed");
            ctx.put(CTX_ACTION_SUFFIX, "from");
        }
        return ctx;
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        // The collection id is fetched and the target users are resolved using this collection id.
        Set<String> targetUsers = new HashSet<>();
        // Get the collection Id
        getSubscriptions(eventRecord.getContext()).getUsernames().forEach(targetUsers::add);

        return targetUsers.stream();
    }
}
