/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.notification.transformer;

import static org.nuxeo.ecm.notification.resolver.TaskAssignedResolver.ACCEPTED_EVENTS;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.RECIPIENTS_KEY;
import static org.nuxeo.ecm.platform.task.TaskService.TASK_INSTANCE_EVENT_PROPERTIES_KEY;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.platform.task.Task;

/**
 * Extract the list of assigned users from the context of the event and add the id of the task document.
 *
 * @since 0.1
 */
public class TaskEventTransformer extends EventTransformer {

    public static final String DELIMITER = ":";

    public static final String TASK_ID_KEY = "taskId";

    @Override
    public boolean accept(Event event) {
        // Accepts only task events related to assignment
        return ACCEPTED_EVENTS.contains(event.getName());
    }

    @Override
    public Map<String, String> buildEventRecordContext(Event event) {
        Map<String, String> ctx = new HashMap<>();

        // Extract the list of recipients
        String[] recipients = (String[]) event.getContext().getProperty(RECIPIENTS_KEY);
        ctx.put(RECIPIENTS_KEY, String.join(DELIMITER, recipients));

        // Extract the task id
        Task task = (Task) event.getContext().getProperty(TASK_INSTANCE_EVENT_PROPERTIES_KEY);
        if (task != null) {
            ctx.put(TASK_ID_KEY, task.getId());
        }
        return ctx;
    }
}
