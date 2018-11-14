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

package org.nuxeo.ecm.notification.message;

import java.util.HashMap;
import java.util.Map;

import org.apache.avro.reflect.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.notification.resolver.Resolver;

public class Notification {

    public static final String ORIGINATING_USER = "originatingUser";

    public static final String ORIGINATING_EVENT = "originatingEvent";

    protected String id;

    protected String username;

    protected String resolverId;

    @Nullable
    protected String sourceId;

    @Nullable
    protected String sourceRepository;

    protected Map<String, String> context = new HashMap<>();

    public void addTargetUsername(String username) {
        this.username = username;
    }

    protected Notification() {
        // Empty constructor for Avro
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getResolverId() {
        return resolverId;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceRepository() {
        return sourceRepository;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }

    public static class NotificationBuilder {
        Notification notif;

        protected NotificationBuilder() {
            notif = new Notification();
        }

        public NotificationBuilder fromEvent(EventRecord eventRecord) {
            if (eventRecord.getDocumentSourceId() != null) {
                withSourceId(eventRecord.documentSourceId);
                withSourceRepository(eventRecord.documentSourceRepository);
            }

            withCtx(ORIGINATING_EVENT, eventRecord.getEventName());
            withCtx(ORIGINATING_USER, eventRecord.getUsername());

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

        public NotificationBuilder withSourceRepository(String repository) {
            notif.sourceRepository = repository;
            return this;
        }

        public NotificationBuilder withSourceId(String sourceId) {
            notif.sourceId = sourceId;
            return this;
        }

        public NotificationBuilder withCtx(String key, String value) {
            notif.context.put(key, value);
            return this;
        }

        public NotificationBuilder withCtx(Map<String, String> context) {
            notif.context.putAll(context);
            return this;
        }

        public Notification build() {
            notif.id = String.format("%s:%s", notif.resolverId, notif.username);
            return notif;
        }
    }
}
