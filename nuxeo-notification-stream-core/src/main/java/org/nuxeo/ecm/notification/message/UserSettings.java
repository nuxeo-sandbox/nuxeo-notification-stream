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

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.notification.model.UserNotifierSettings;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * A message representing the notification user settings.
 *
 * @since XXX
 */
public class UserSettings implements Serializable {

    public UserSettings() {
        // Empty constructor for Avro decoder
    }

    protected String id;

    protected String username;

    protected Map<String, UserNotifierSettings> settingsMap;

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

    public Map<String, UserNotifierSettings> getSettingsMap() {
        if (settingsMap == null) {
            settingsMap = new HashMap<>();
        }
        return settingsMap;
    }

    public byte[] encode() {
        return Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, UserSettings.class).encode(this);
    }

    public static UserSettingsBuilder builder() {
        return new UserSettingsBuilder();
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

    public static class UserSettingsBuilder {
        UserSettings us;

        protected UserSettingsBuilder() {
            us = new UserSettings();
        }

        public UserSettingsBuilder withUsername(String username) {
            us.username = username;
            return this;
        }

        public UserSettingsBuilder withSettings(Map<String, UserNotifierSettings> notifiersSettings) {
            us.getSettingsMap().putAll(notifiersSettings);
            return this;
        }

        public UserSettingsBuilder withSetting(String resolverId, UserNotifierSettings notifiersSettings) {
            us.getSettingsMap().put(resolverId, notifiersSettings);
            return this;
        }

        public UserSettings build() {
            us.id = UUID.randomUUID().toString();
            return us;
        }
    }
}
