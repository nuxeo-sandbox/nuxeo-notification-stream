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
import java.util.HashMap;
import java.util.Map;

/**
 * A message representing the notification user settings.
 *
 * @since XXX
 */
public class UserSettingsRecord implements Serializable {

    public UserSettingsRecord() {
        // Empty constructor for Avro decoder
    }

    protected String username;

    protected Map<String, UserResolverSettings> settingsMap;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, UserResolverSettings> getSettingsMap() {
        if (settingsMap == null) {
            settingsMap = new HashMap<>();
        }
        return settingsMap;
    }

    public void setSettingsMap(Map<String, UserResolverSettings> settingsMap) {
        this.settingsMap = settingsMap;
    }
}
