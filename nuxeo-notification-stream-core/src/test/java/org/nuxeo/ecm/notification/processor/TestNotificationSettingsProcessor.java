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
package org.nuxeo.ecm.notification.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.notification.NotificationComponent;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.NotificationSettingsService;
import org.nuxeo.ecm.notification.NotificationStreamConfig;
import org.nuxeo.ecm.notification.TestNotificationHelper;
import org.nuxeo.ecm.notification.message.UserSettings;
import org.nuxeo.ecm.notification.model.UserNotifierSettings;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.kv.KeyValueService;
import org.nuxeo.runtime.kv.KeyValueStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/basic-contrib.xml")
public class TestNotificationSettingsProcessor {

    @Inject
    protected NotificationStreamConfig nsc;

    @Inject
    protected NotificationSettingsService nss;

    @Inject
    protected CodecService codecService;

    @Test
    public void testTopologyExecution() {
        // Create a record in the stream in input of the notification settings processor
        LogManager logManager = getUserSettingsLogManager();
        assertThat(logManager.getAppender(nsc.getNotificationSettingsInputStream())).isNotNull();

        LogAppender<Record> appender = logManager.getAppender(nsc.getNotificationSettingsInputStream());
        UserSettings record = buildUserSettings();

        // Write the record in the log
        appender.append("settings",
                Record.of("settings", codecService.getCodec(DEFAULT_CODEC, UserSettings.class).encode(record)));

        // Wait for the completion and check the result stored in the KVS
        TestNotificationHelper.waitProcessorsCompletion();
        KeyValueStore store = Framework.getService(KeyValueService.class)
                                       .getKeyValueStore(NotificationComponent.KVS_SETTINGS);

        Codec<UserNotifierSettings> codec = codecService.getCodec(DEFAULT_CODEC, UserNotifierSettings.class);
        byte[] userSettingsBytes = store.get("user1:fileCreated");
        UserNotifierSettings userSettings = codec.decode(userSettingsBytes);
        assertThat(userSettings.getSettings().get("log")).isTrue();
        assertThat(userSettings.getSettings().get("inApp")).isFalse();
        userSettingsBytes = store.get("user1:fileUpdated");
        userSettings = codec.decode(userSettingsBytes);
        assertThat(userSettings.getSettings().get("log")).isTrue();
        assertThat(userSettings.getSettings().get("inApp")).isFalse();
    }

    @Test
    public void testUserSettingsSave() {
        UserSettings record = buildUserSettings();
        nss.updateSettings(record.getUsername(), record.getSettingsMap());

        TestNotificationHelper.waitProcessorsCompletion();

        UserSettings settings = nss.getResolverSettings(record.getUsername());
        assertThat(settings.getSettings("fileCreated").getSettings().get("log")).isTrue();
        assertThat(settings.getSettings("fileCreated").getSettings().get("inApp")).isFalse();
    }

    protected LogManager getUserSettingsLogManager() {
        return nsc.getLogManager(nsc.getLogConfigNotification());
    }

    protected UserSettings buildUserSettings() {
        UserSettings.UserSettingsBuilder builder = UserSettings.builder().withUsername("user1");

        UserNotifierSettings settings = new UserNotifierSettings();
        settings.getSettings().put("log", true);
        settings.getSettings().put("inApp", false);

        builder.withSetting("fileCreated", settings);
        builder.withSetting("fileUpdated", settings);

        return builder.build();
    }
}
