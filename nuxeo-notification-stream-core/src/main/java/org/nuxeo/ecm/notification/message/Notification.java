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

import static org.nuxeo.ecm.core.schema.types.constraints.Constraint.MESSAGES_BUNDLE;
import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_DOC_ID;
import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_DOC_REPO;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.notification.entities.TextEntitiesReplacer;
import org.nuxeo.ecm.notification.entities.TextEntitiesSupplier;
import org.nuxeo.ecm.notification.entities.TextEntity;
import org.nuxeo.ecm.notification.resolver.Resolver;

/**
 * @since 0.1
 */
public class Notification {

    public static final String ORIGINATING_USER = "originatingUser";

    public static final String ORIGINATING_EVENT = "originatingEvent";

    public static final String CREATED_AT = "createdAt";

    protected String id;

    protected String username;

    protected String resolverId;

    protected String resolverMessage;

    protected String message;

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

    public String getMessage() {
        return message;
    }

    public String getOriginatingUser() {
        return getContext().get(ORIGINATING_USER);
    }

    public List<TextEntity> entities;

    public Map<String, String> getContext() {
        return context;
    }

    public List<TextEntity> getEntities() {
        return entities;
    }

    public DocumentRef getSourceRef() {
        String sourceId = getSourceId();
        return sourceId.startsWith("/") ? new PathRef(sourceId) : new IdRef(sourceId);
    }

    public String getSourceId() {
        return getContext().get(SOURCE_DOC_ID);
    }

    public String getSourceRepository() {
        return getContext().get(SOURCE_DOC_REPO);
    }

    public String getCreatedAt() {
        return getContext().get(CREATED_AT);
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
            withCtx(eventRecord.getContext());
            withCtx(ORIGINATING_EVENT, eventRecord.getEventName());
            withCtx(ORIGINATING_USER, eventRecord.getUsername());
            withCtx(CREATED_AT, String.valueOf(eventRecord.getTime()));

            return this;
        }

        public NotificationBuilder withResolver(Resolver resolver, Locale locale) {
            notif.resolverId = resolver.getId();
            return withResolverMessage(resolver.getMessageKey(), locale);
        }

        public NotificationBuilder withResolverMessage(String messageKey, Locale locale) {
            // Fetch the message using the key
            notif.resolverMessage = getI18nMessage(locale, messageKey);
            return this;
        }

        public NotificationBuilder withMessage(String message) {
            notif.message = message;
            return this;
        }

        public NotificationBuilder withUsername(String username) {
            notif.addTargetUsername(username);
            return this;
        }

        public NotificationBuilder withSourceRepository(String repository) {
            withCtx(SOURCE_DOC_REPO, repository);
            return this;
        }

        public NotificationBuilder withSourceId(String sourceId) {
            withCtx(SOURCE_DOC_ID, sourceId);
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

        public NotificationBuilder computeMessage() {
            TextEntitiesReplacer replacer = TextEntitiesReplacer.from(notif.resolverMessage, notif.getContext());
            notif.message = replacer.replaceCtxKeys();
            return this;
        }

        public NotificationBuilder prepareEntities() {
            TextEntitiesReplacer replacer = TextEntitiesReplacer.from(notif.message);
            notif.entities = replacer.buildTextEntities();
            return this;
        }

        public NotificationBuilder resolveEntities() {
            TextEntitiesSupplier.resolve(notif);
            return this;
        }

        public Notification build() {
            notif.id = String.format("%s:%s", notif.resolverId, notif.username);
            return notif;
        }

        protected String getI18nMessage(Locale locale, String messageKey) {
            String messageI18n = I18NUtils.getMessageString(MESSAGES_BUNDLE, messageKey, null, locale);
            if (StringUtils.isEmpty(messageI18n)) {
                messageI18n = messageKey;
            }
            return messageI18n;
        }
    }
}
