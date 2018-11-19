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

package org.nuxeo.ecm.notification.notifier;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.notification.message.Notification;

public class CounterNotifier extends Notifier {
    private static final Logger log = LogManager.getLogger(CounterNotifier.class);

    private static final Object sync = new Object();

    public static Integer processed = 0;

    public static final Map<String, String> fullCtx = new HashMap<>();

    public CounterNotifier(NotifierDescriptor desc) {
        super(desc);
    }

    @Override
    public void process(Notification notification) {
        synchronized (sync) {
            processed++;
            fullCtx.putAll(notification.getContext());

            log.warn(getName() + ":" + notification.toString());
        }
    }

    public static void reset() {
        synchronized (sync) {
            processed = 0;
            fullCtx.clear();
        }
    }
}
