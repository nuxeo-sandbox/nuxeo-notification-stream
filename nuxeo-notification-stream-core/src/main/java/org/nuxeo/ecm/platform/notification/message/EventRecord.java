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

import com.sun.xml.internal.bind.v2.model.core.ID;
import org.apache.avro.reflect.Nullable;

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

    public EventRecord(String eventName, String username){
        this.eventName = eventName;
        this.username = username;
    }

    public EventRecord(String eventName, String docId, String username){
        this.eventName = eventName;
        this.docId = docId;
        this.username = username;
    }

    protected String eventName;

    @Nullable
    protected String docId;
    
    protected String username;

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
