/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.processors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.platform.notification.NotificationComponent.KVS_SETTINGS;
import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.notification.NotificationFeature;
import org.nuxeo.ecm.platform.notification.NotificationStreamConfig;
import org.nuxeo.ecm.platform.notification.TestNotificationHelper;
import org.nuxeo.ecm.platform.notification.message.UserResolverSettings;
import org.nuxeo.ecm.platform.notification.message.UserSettingsRecord;
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
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/dummy-contrib.xml")
public class TestNotificationSettingsProcessor {

    @Inject
    protected NotificationStreamConfig notificationStreamConfig;

    @Inject
    protected CodecService codecService;

    @Test
    public void testTopologyExecution() throws InterruptedException {
        // Create a record in the stream in input of the notification settings processor
        LogManager logManager = notificationStreamConfig.getLogManager(notificationStreamConfig.getLogConfigSettings());
        assertThat(logManager.getAppender(notificationStreamConfig.getNotificationSettingsInputStream())).isNotNull();

        LogAppender<Record> appender = logManager.getAppender(
                notificationStreamConfig.getNotificationSettingsInputStream());
        UserSettingsRecord record = new UserSettingsRecord();
        record.setUsername("user1");
        UserResolverSettings settings = new UserResolverSettings();
        settings.getSettings().put("log", true);
        settings.getSettings().put("inApp", false);
        record.getSettingsMap().put("fileCreated", settings);
        record.getSettingsMap().put("fileUpdated", settings);

        // Write the record in the log
        appender.append("settings",
                Record.of("settings", codecService.getCodec(DEFAULT_CODEC, UserSettingsRecord.class).encode(record)));

        // Wait for the completion and check the result stored in the KVS
        TestNotificationHelper.awaitCompletion(logManager, 5, TimeUnit.SECONDS);
        KeyValueStore store = Framework.getService(KeyValueService.class).getKeyValueStore(KVS_SETTINGS);

        Codec<UserResolverSettings> codec = codecService.getCodec(DEFAULT_CODEC, UserResolverSettings.class);
        byte[] userSettingsBytes = store.get("user1:fileCreated");
        UserResolverSettings userSettings = codec.decode(userSettingsBytes);
        assertThat(userSettings.getSettings().get("log")).isTrue();
        assertThat(userSettings.getSettings().get("inApp")).isFalse();
        userSettingsBytes = store.get("user1:fileUpdated");
        userSettings = codec.decode(userSettingsBytes);
        assertThat(userSettings.getSettings().get("log")).isTrue();
        assertThat(userSettings.getSettings().get("inApp")).isFalse();
    }
}
