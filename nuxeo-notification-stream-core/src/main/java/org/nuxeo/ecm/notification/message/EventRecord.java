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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.avro.reflect.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * A message representing an Event.
 *
 * @since XXX
 */
public class EventRecord implements Serializable {

    private static final long serialVersionUID = 0L;

    protected EventRecord() {
        // Empty constructor for Avro decoder
    }

    protected String id;

    protected String eventName;

    @Nullable
    protected String documentSourceId;

    @Nullable
    protected String documentSourceType;

    @Nullable
    protected String documentSourceRepository;

    protected String username;

    protected Map<String, String> context = new HashMap<>();

    public String getEventName() {
        return eventName;
    }

    public String getDocumentSourceId() {
        return documentSourceId;
    }

    public String getDocumentSourceType() {
        return documentSourceType;
    }

    public String getUsername() {
        return username;
    }

    public String getRepository() {
        return StringUtils.isBlank(documentSourceRepository)
                ? Framework.getService(RepositoryManager.class).getDefaultRepositoryName()
                : documentSourceRepository;
    }

    public Map<String, String> getContext() {
        return new HashMap<>(context);
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

    public static EventRecordBuilder builder() {
        return new EventRecordBuilder();
    }

    public static class EventRecordBuilder {
        EventRecord record;

        protected EventRecordBuilder() {
            record = new EventRecord();
        }

        public EventRecordBuilder withDocument(DocumentModel doc) {
            return withDocumentId(doc.getId()).withDocumentRepository(doc.getRepositoryName())
                                              .withDocumentType(doc.getType());
        }

        public EventRecordBuilder withDocumentId(String docId) {
            record.documentSourceId = docId;
            return this;
        }

        public EventRecordBuilder withDocumentType(String docType) {
            record.documentSourceType = docType;
            return this;
        }

        public EventRecordBuilder withDocumentRepository(String repository) {
            record.documentSourceRepository = repository;
            return this;
        }

        public EventRecordBuilder withEventName(String eventName) {
            record.eventName = eventName;
            return this;
        }

        public EventRecordBuilder withUsername(String username) {
            record.username = username;
            return this;
        }

        public EventRecordBuilder withContext(Map<String, String> context) {
            record.context.putAll(context);
            return this;
        }

        public EventRecord build() {
            record.id = UUID.randomUUID().toString();
            return record;
        }
    }
}
