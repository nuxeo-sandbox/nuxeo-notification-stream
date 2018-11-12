/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.notification.dispatcher.Dispatcher;
import org.nuxeo.ecm.platform.notification.model.UserDispatcherSettings;

/**
 * Service for managing the notification settings.
 *
 * @since XXX
 */
public interface NotificationSettingsService {
    void updateSettings(String username, Map<String, UserDispatcherSettings> userSettings);

    Map<String, UserDispatcherSettings> getResolverSettings(String username);

    boolean hasSpecificSettings(String username, String resolverId);

    List<Dispatcher> getSelectedDispatchers(String username, String resolverId);
}
