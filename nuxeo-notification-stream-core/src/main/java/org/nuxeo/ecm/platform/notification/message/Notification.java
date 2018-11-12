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

package org.nuxeo.ecm.platform.notification.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.avro.reflect.Nullable;
import org.nuxeo.ecm.platform.notification.NotificationSettingsService;
import org.nuxeo.ecm.platform.notification.dispatcher.Dispatcher;
import org.nuxeo.ecm.platform.notification.resolver.Resolver;
import org.nuxeo.runtime.api.Framework;

public class Notification {

    protected String id;

    protected String username;

    protected String resolverId;

    @Nullable
    protected String sourceId;

    protected Map<String, Serializable> context = new HashMap<>();

    protected List<String> dispatchers = new ArrayList<>();

    public void addTargetUsername(String username) {
        this.username = username;
    }

    public Notification() {
        //
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getResolverId() {
        return resolverId;
    }

    public List<String> getDispatchers() {
        return dispatchers;
    }

    public boolean hasDispatchers() {
        return !getDispatchers().isEmpty();
    }

    public Map<String, Serializable> getContext() {
        return context;
    }

    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }

    public static class NotificationBuilder {
        Notification notif;

        public NotificationBuilder() {
            notif = new Notification();
        }

        public NotificationBuilder fromEvent(EventRecord eventRecord) {
            if (eventRecord.getDocumentSourceId() != null) {
                withSourceId(eventRecord.getDocumentSourceId());
            }

            return this;
        }

        public NotificationBuilder withSourceId(String sourceId) {
            notif.sourceId = sourceId;
            return this;
        }

        public NotificationBuilder withResolver(Resolver resolver) {
            notif.resolverId = resolver.getId();
            return this;
        }

        public NotificationBuilder withUsername(String username) {
            notif.addTargetUsername(username);
            return this;
        }

        public NotificationBuilder withCtx(String key, Serializable value) {
            notif.context.put(key, value);
            return this;
        }

        public NotificationBuilder withCtx(Map<String, Serializable> context) {
            notif.context.putAll(context);
            return this;
        }

        public Notification build() {
            notif.id = UUID.randomUUID().toString();
            notif.dispatchers = Framework.getService(NotificationSettingsService.class)
                                         .getSelectedDispatchers(notif.username, notif.resolverId)
                                         .stream()
                                         .map(Dispatcher::getName)
                                         .collect(Collectors.toList());

            return notif;
        }
    }
}
