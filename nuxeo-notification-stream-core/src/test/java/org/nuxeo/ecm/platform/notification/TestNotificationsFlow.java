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

package org.nuxeo.ecm.platform.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.platform.notification.NotificationComponent.KVS_SETTINGS;
import static org.nuxeo.ecm.platform.notification.resolver.SubscribableResolver.KVS_SUBSCRIPTIONS;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.notification.dispatcher.CounterDispatcher;
import org.nuxeo.ecm.platform.notification.model.UserDispatcherSettings;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/basic-contrib.xml")
public class TestNotificationsFlow {

    @Inject
    protected NotificationService ns;

    @Inject
    protected NotificationSettingsService nss;

    @Inject
    protected NotificationStreamConfig nsc;

    @Inject
    protected CoreSession session;

    @Before
    public void before() {
        // Reset counter dispatcher
        CounterDispatcher.reset();

        // Clean KVS
        TestNotificationHelper.clearKVS(KVS_SUBSCRIPTIONS);
        TestNotificationHelper.clearKVS(KVS_SETTINGS);
    }

    @Test
    public void testUserChangingSettings() throws InterruptedException {
        assertThat(CounterDispatcher.processed).isEqualTo(0);

        createSampleFile();
        waitForAsync();
        assertThat(CounterDispatcher.processed).isEqualTo(0);

        ns.subscribe("myUser", "fileCreated", Collections.emptyMap());
        waitForAsync();

        createSampleFile();
        waitForAsync();

        // First call, only one event should have been processed
        assertThat(CounterDispatcher.processed).isEqualTo(1);

        CounterDispatcher.reset();
        assertThat(CounterDispatcher.processed).isEqualTo(0);

        // User change settings to enable disp2
        Map<String, UserDispatcherSettings> settings = nss.getResolverSettings("myUser");
        settings.get("fileCreated").enable("log");
        nss.updateSettings("myUser", settings);
        waitForAsync();

        createSampleFile();
        waitForAsync();
        assertThat(CounterDispatcher.processed).isEqualTo(2);
    }

    protected DocumentModel createSampleFile() {
        // Create test docModele
        DocumentModel doc = session.createDocumentModel("/", RandomStringUtils.randomAlphabetic(5), "File");
        // Set target users on metadata
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        return doc;
    }

    protected void waitForAsync() throws InterruptedException {
        TestNotificationHelper.waitForEvents();

        LogManager lm = nsc.getLogManager(nsc.getLogConfigNotification());
        TestNotificationHelper.waitProcessorsCompletion(lm, Duration.ofSeconds(5));

        lm = nsc.getLogManager(nsc.getLogConfigNotification());
        TestNotificationHelper.waitProcessorsCompletion(lm, Duration.ofSeconds(5));

        lm = nsc.getLogManager(nsc.getLogConfigNotification());
        TestNotificationHelper.waitProcessorsCompletion(lm, Duration.ofSeconds(5));
    }
}
