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
package org.nuxeo.ecm.notification;

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
