/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Nuxeo
 */
package org.nuxeo.ecm.notification;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.notification.message.UserSettings;
import org.nuxeo.ecm.notification.model.UserNotifierSettings;
import org.nuxeo.ecm.notification.notifier.Notifier;

/**
 * Service for managing the notification settings.
 *
 * @since 0.1
 */
public interface NotificationSettingsService {
    void updateSettings(String username, Map<String, UserNotifierSettings> userSettings);

    UserSettings getResolverSettings(String username);

    boolean hasSpecificSettings(String username, String resolverId);

    List<Notifier> getSelectedNotifiers(String username, String resolverId);
}
