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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.notification.message.UserSettings;
import org.nuxeo.ecm.notification.notifier.CounterNotifier;
import org.nuxeo.ecm.notification.resolver.SubscribableResolver;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/basic-contrib.xml")
public class TestNotificationsFlow {

    @Inject
    protected NotificationService ns;

    @Inject
    protected NotificationSettingsService nss;

    @Inject
    protected CoreSession session;

    @Before
    public void before() {
        // Clean KVS
        TestNotificationHelper.clearKVS(SubscribableResolver.KVS_SUBSCRIPTIONS);
        TestNotificationHelper.clearKVS(NotificationComponent.KVS_SETTINGS);
    }

    @Test
    public void testUserChangingSettings() {
        assertThat(CounterNotifier.processed).isEqualTo(0);

        createSampleFile();
        TestNotificationHelper.waitProcessorsCompletion();
        assertThat(CounterNotifier.processed).isEqualTo(0);

        ns.subscribe("myUser", "fileCreated", Collections.emptyMap());
        TestNotificationHelper.waitProcessorsCompletion();

        createSampleFile();
        TestNotificationHelper.waitProcessorsCompletion();

        // First call, only one event should have been processed
        assertThat(CounterNotifier.processed).isEqualTo(1);

        CounterNotifier.reset();
        assertThat(CounterNotifier.processed).isEqualTo(0);

        // User change settings to enable disp2
        UserSettings settings = nss.getResolverSettings("myUser");
        settings.getSettings("fileCreated").enable("log");
        nss.updateSettings("myUser", settings.getSettingsMap());
        TestNotificationHelper.waitProcessorsCompletion();

        createSampleFile();
        TestNotificationHelper.waitProcessorsCompletion();
        assertThat(CounterNotifier.processed).isEqualTo(2);
    }

    protected DocumentModel createSampleFile() {
        // Create test docModel
        DocumentModel doc = session.createDocumentModel("/", RandomStringUtils.randomAlphabetic(5), "File");
        // Set target users on metadata
        doc = session.createDocument(doc);

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        return doc;
    }
}
