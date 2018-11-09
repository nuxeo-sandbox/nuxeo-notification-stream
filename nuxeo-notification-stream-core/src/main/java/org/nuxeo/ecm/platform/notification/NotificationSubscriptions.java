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

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

public class NotificationSubscriptions {
    protected HashSet<String> usernames = new HashSet<>();

    protected NotificationSubscriptions() {

    }

    public Stream<String> getUsernames() {
        return usernames.stream();
    }

    public NotificationSubscriptions addUsername(String username) {
        usernames.add(username);
        return this;
    }

    public static NotificationSubscriptions withUser(String username) {
        NotificationSubscriptions subs = new NotificationSubscriptions();
        subs.addUsername(username);
        return subs;
    }

    public static NotificationSubscriptions withUsers(Collection<String> usernames) {
        NotificationSubscriptions subs = new NotificationSubscriptions();
        usernames.forEach(subs::addUsername);
        return subs;
    }
}
