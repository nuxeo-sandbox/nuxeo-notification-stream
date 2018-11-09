/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.message;

import java.io.Serializable;
import java.util.UUID;

import org.apache.avro.reflect.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A message representing an Event.
 *
 * @since XXX
 */
public class EventRecord implements Serializable {

    private static final long serialVersionUID = 0L;

    public EventRecord() {
        // Empty constructor for Avro decoder
    }

    public EventRecord(String eventName, String username) {
        this(eventName, null, null, username);
    }

    public EventRecord(String eventName, String documentSourceId, String documentSourceType, String username) {
        this.id = UUID.randomUUID().toString();
        this.eventName = eventName;
        this.documentSourceId = documentSourceId;
        this.documentSourceType = documentSourceType;
        this.username = username;
    }

    protected String id;

    protected String eventName;

    @Nullable
    protected String documentSourceId;

    @Nullable
    protected String documentSourceType;

    protected String username;

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDocumentSourceId() {
        return documentSourceId;
    }

    public void setDocumentSourceId(String documentSourceId) {
        this.documentSourceId = documentSourceId;
    }

    public String getDocumentSourceType() {
        return documentSourceType;
    }

    public void setDocumentSourceType(String documentSourceType) {
        this.documentSourceType = documentSourceType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
}
