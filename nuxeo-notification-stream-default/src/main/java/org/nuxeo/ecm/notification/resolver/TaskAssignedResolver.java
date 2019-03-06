/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.resolver;

import static org.nuxeo.ecm.notification.transformer.TaskEventTransformer.DELIMITER;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_ASSIGNED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_DELEGATED;
import static org.nuxeo.ecm.platform.task.TaskEventNames.WORKFLOW_TASK_REASSIGNED;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.notification.message.EventRecord;

/**
 * Resolver for the notifications sent when a task is assigned to a user. It triggers a notification also when a task is
 * reassigned or delegated.
 *
 * @since 0.1
 */
public class TaskAssignedResolver extends Resolver {

    public static final List<String> ACCEPTED_EVENTS = Arrays.asList(WORKFLOW_TASK_ASSIGNED, WORKFLOW_TASK_REASSIGNED,
            WORKFLOW_TASK_DELEGATED);

    @Override
    public boolean accept(EventRecord eventRecord) {
        return ACCEPTED_EVENTS.contains(eventRecord.getEventName());
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        // Extract the target users from the context of the events
        if (eventRecord.getContext().get(RECIPIENTS_KEY) != null) {
            return Arrays.stream(eventRecord.getContext().get(RECIPIENTS_KEY).split(DELIMITER))
                         .filter(k -> StringUtils.isNotBlank(k));
        }
        return null;
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        return Collections.emptyMap();
    }
}
