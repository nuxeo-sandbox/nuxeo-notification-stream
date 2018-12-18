package org.nuxeo.ecm.notification.entities;

import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.notification.message.Notification;

public interface TextEntityFormatter {
    String format(String message, List<TextEntity> entities);

    static String format(Class<? extends TextEntityFormatter> clazz, String message, List<TextEntity> entities) {
        try {
            return clazz.newInstance().format(message, entities);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new NuxeoException(e);
        }
    }

    static String format(Class<? extends TextEntityFormatter> clazz, Notification notif) {
        return format(clazz, notif.getMessage(), notif.getEntities());
    }
}
