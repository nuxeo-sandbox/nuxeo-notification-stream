/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

@XObject("settings")
public class SettingsDescriptor implements Descriptor {
    @XNode("@id")
    protected String id;

    @XNodeMap(value = "notifier", key = "@name", type = HashMap.class, componentType = NotifierSetting.class)
    protected Map<String, NotifierSetting> settings;

    @Override
    public String getId() {
        return id;
    }

    public Map<String, NotifierSetting> getSettings() {
        return new HashMap<>(settings);
    }

    public NotifierSetting getSetting(String notifier) {
        return settings.getOrDefault(notifier, new NotifierSetting());
    }

    public boolean isEnable(String notifier) {
        return getSetting(notifier).isEnabled();
    }

    public boolean isDefault(String notifier) {
        return getSetting(notifier).isDefault();
    }

    @Override
    public Descriptor merge(Descriptor other) {
        // TODO
        return null;
    }


    @XObject("notifier")
    public static class NotifierSetting {
        @XNode("@enabled")
        protected boolean isEnabled = true;

        @XNode("@default")
        protected boolean isDefault = false;

        public boolean isEnabled() {
            return isEnabled;
        }

        public boolean isDefault() {
            return isDefault;
        }
    }
}
