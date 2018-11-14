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

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.stream.StreamHelper;

/**
 * @since XXX
 */
public class TestNotificationHelper {

    /**
     * Await for notification processor to drain and stop all his computations
     *
     * @return {@code true} if computations are stopped during the timeout delay.
     */
    public static boolean waitProcessorsCompletion() {
        return StreamHelper.drainAndStop();
    }

    public static void clearKVS(String name) {
        NotificationComponent nc = (NotificationComponent) Framework.getService(NotificationService.class);
        ((KeyValueStoreProvider) nc.getKeyValueStore(name)).clear();
    }
}
