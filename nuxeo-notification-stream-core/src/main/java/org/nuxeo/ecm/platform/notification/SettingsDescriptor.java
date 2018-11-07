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

    @XNodeMap(value = "dispatcher", key = "@name", type = HashMap.class, componentType = DispatcherSetting.class)
    protected Map<String, DispatcherSetting> settings;

    @Override
    public String getId() {
        return id;
    }

    public Map<String, DispatcherSetting> getSettings() {
        return new HashMap<>(settings);
    }

    public DispatcherSetting getSetting(String dispatcher) {
        return settings.getOrDefault(dispatcher, new DispatcherSetting());
    }

    public boolean isEnable(String dispatcher) {
        return getSetting(dispatcher).isEnabled();
    }

    public boolean isDefault(String dispatcher) {
        return getSetting(dispatcher).isDefault();
    }

    @Override
    public Descriptor merge(Descriptor other) {
        // TODO
        return null;
    }


    @XObject("dispatcher")
    public static class DispatcherSetting {
        @XNode("@enable")
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
