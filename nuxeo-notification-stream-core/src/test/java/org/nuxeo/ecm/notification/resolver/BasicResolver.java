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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Basic resolver that accepts only a limited set of events and return a list of three users for the subscribers.
 *
 * @since 0.1
 */
public class BasicResolver extends Resolver {

    public static final int TARGET_USERS = 3;

    public static final List<String> TARGET_EVENTS = Arrays.asList(DOCUMENT_CREATED, DOCUMENT_UPDATED);

    @Override
    public boolean accept(EventRecord eventRecord) {
        return TARGET_EVENTS.contains(eventRecord.getEventName());
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        return IntStream.range(0, TARGET_USERS) //
                        .boxed()
                        .map(s -> "user" + s);
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        return Collections.emptyMap();
    }
}
