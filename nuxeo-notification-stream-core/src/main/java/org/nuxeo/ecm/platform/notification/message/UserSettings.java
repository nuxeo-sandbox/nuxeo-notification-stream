/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.message;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.platform.notification.model.UserResolverSettings;
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

    protected Map<String, UserResolverSettings> settingsMap;

    public String getUsername() {
        return username;
    }

    public String getId() {
        return id;
    }

    public Map<String, UserResolverSettings> getSettingsMap() {
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

        public UserSettingsBuilder withSettings(Map<String, UserResolverSettings> dispatchersSettings) {
            us.getSettingsMap().putAll(dispatchersSettings);
            return this;
        }

        public UserSettingsBuilder withSetting(String resolverId, UserResolverSettings dispatchersSetting) {
            us.getSettingsMap().put(resolverId, dispatchersSetting);
            return this;
        }

        public UserSettings build() {
            us.id = UUID.randomUUID().toString();
            return us;
        }
    }
}
