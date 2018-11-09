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
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.notification.dispatcher.Dispatcher;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.ecm.platform.notification.resolver.ComplexSubsKeyResolver;
import org.nuxeo.ecm.platform.notification.resolver.FileCreatedResolver;
import org.nuxeo.ecm.platform.notification.resolver.FileUpdatedResolver;
import org.nuxeo.ecm.platform.notification.resolver.Resolver;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/dummy-contrib.xml")
public class TestNotificationService {
    @Inject
    NotificationService notif;

    @Inject
    CoreSession session;

    @Test
    public void testRegistration() {
        assertThat(notif.getDispatcher("dummy")).isNull();
        assertThat(notif.getDispatcher("inApp")).isNotNull();
        assertThat(notif.getDispatchers()).hasSize(2);

        assertThat(notif.getResolver("dummy")).isNull();
        assertThat(notif.getResolver("fileCreated")) //
                                                    .isNotNull();
        assertThat(notif.getResolver("fileUpdated")) //
                                                    .isNotNull();
        assertThat(notif.getResolvers()).hasSize(3);
    }

    @Test
    public void testDispatcherInit() {
        Dispatcher log = notif.getDispatcher("log");
        assertThat(log.getProperty("dummy", "john")).isEqualTo("john");
        assertThat(log.getProperty("dummy")).isNull();
        assertThat(log.getProperty("my-secret-key")).isEqualTo("my-secret-value");
    }

    @Test
    public void testResolverResolution() {
        EventRecord eventRecord = new EventRecord(DOCUMENT_UPDATED, "0000-0000-0000", "File",
                session.getPrincipal().getName());
        assertThat(notif.getResolvers(eventRecord)).hasSize(1).first().isInstanceOf(FileUpdatedResolver.class);

        eventRecord.setEventName(DOCUMENT_CREATED);
        assertThat(notif.getResolvers(eventRecord)).hasSize(1).first().isInstanceOf(FileCreatedResolver.class);

        eventRecord.setEventName(DOCUMENT_CHECKEDIN);
        assertThat(notif.getResolvers(eventRecord)).hasSize(0);
    }

    @Test
    public void testSubscriptionsStorage() {
        String resolverId = "complexKey";
        Map<String, String> ctx = Collections.emptyMap();
        String firstUser = "dummy";
        String secondUser = "Johnny";
        String thirdUser = "Arthur";

        NotificationSubscriptions subs = notif.getSubscribtions(resolverId, ctx);

        assertThat(subs).isNull();

        notif.subscribe(firstUser, resolverId, ctx);
        subs = notif.getSubscribtions(resolverId, ctx);

        assertThat(subs).isNotNull();
        assertThat(subs.getUsernames()).containsExactly(firstUser);

        notif.subscribe(secondUser, resolverId, ctx);
        subs = notif.getSubscribtions(resolverId, ctx);
        assertThat(subs.getUsernames()).containsExactly(firstUser, secondUser);

        ctx = Collections.singletonMap(ComplexSubsKeyResolver.NAME_FIELD, "newName");
        notif.subscribe(thirdUser, resolverId, ctx);
        subs = notif.getSubscribtions(resolverId, ctx);
        assertThat(subs.getUsernames()).containsExactly(thirdUser);
    }

    @Test
    public void testResolverSubscriptions() {
        String resolverId = "fileCreated";
        Map<String, String> ctx = Collections.emptyMap();
        EventRecord emptyRecord = new EventRecord();

        Resolver resolver = notif.getResolver(resolverId);
        assertThat(resolver.resolveTargetUsers(emptyRecord)).isEmpty();

        String username = "toto";
        notif.subscribe(username, resolverId, ctx);
        assertThat(resolver.resolveTargetUsers(emptyRecord)).isNotEmpty().containsExactly(username);
    }

    @Test(expected = NuxeoException.class)
    public void testSubscriptionForMissingResolver() {
        notif.getSubscribtions("somethiiiing", null);
        failBecauseExceptionWasNotThrown(NuxeoException.class);
    }

    @Test
    public void testNotificationSettings() {
        NotificationComponent cmp = (NotificationComponent) notif;
        SettingsDescriptor.DispatcherSetting setting = cmp.getSetting("fileCreated").getSetting("inApp");
        assertThat(setting.isDefault()).isTrue();
        assertThat(setting.isEnabled()).isTrue();

        setting = cmp.getSetting("fileCreated").getSetting("unknown");
        assertThat(setting.isDefault()).isFalse();
        assertThat(setting.isEnabled()).isTrue();

        setting = cmp.getSetting("fileUpdated").getSetting("log");
        assertThat(setting.isEnabled()).isFalse();
    }
}
