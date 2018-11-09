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

package org.nuxeo.ecm.platform.notification;

import java.util.Map;

import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.log.LogManager;

public interface NotificationStreamConfig {
    /**
     * Returns Notification's Stream computation topology.
     */
    Topology buildTopology(Map<String, String> options);

    /**
     * Returns stream name which is going to be consumed to create Notification object
     */
    String getEventInputStream();

    /**
     * @return The name of the output stream of the notification processor.
     */
    String getNotificationOutputStream();

    /**
     * @return the name of the subscriptions stream.
     */
    String getNotificationSubscriptionsStream();

    /**
     * Returns the Log manager configured on the notification service.
     */
    LogManager getLogManager();
}
