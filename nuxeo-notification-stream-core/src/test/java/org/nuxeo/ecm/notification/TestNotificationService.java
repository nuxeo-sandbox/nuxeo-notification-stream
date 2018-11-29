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
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CHECKEDIN;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.notification.message.Notification.ORIGINATING_EVENT;
import static org.nuxeo.ecm.notification.message.Notification.ORIGINATING_USER;
import static org.nuxeo.ecm.notification.transformer.AnotherBasicTransformer.KEY_INFO;
import static org.nuxeo.ecm.notification.transformer.BasicTransformer.KEY_EVENT_NAME;
import static org.nuxeo.ecm.notification.transformer.BasicTransformer.KEY_TRANSFORMER_NAME;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.event.impl.EventImpl;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.message.EventRecord.EventRecordBuilder;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.model.Subscribers;
import org.nuxeo.ecm.notification.notifier.Notifier;
import org.nuxeo.ecm.notification.resolver.ComplexSubsKeyResolver;
import org.nuxeo.ecm.notification.resolver.FileCreatedResolver;
import org.nuxeo.ecm.notification.resolver.FileUpdatedResolver;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.notification.resolver.SubscribableResolver;
import org.nuxeo.ecm.notification.transformer.EventTransformer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/basic-contrib.xml")
public class TestNotificationService {
    @Inject
    NotificationService notif;

    @Inject
    NotificationStreamCallback scb;

    @Inject
    CoreSession session;

    @Before
    public void before() {
        TestNotificationHelper.clearKVS(SubscribableResolver.KVS_SUBSCRIPTIONS);
    }

    @Test
    public void testRegistration() {
        assertThat(notif.getNotifier("dummy")).isNull();
        assertThat(notif.getNotifier("inApp")).isNotNull();
        assertThat(notif.getNotifiers()).hasSize(2);

        assertThat(notif.getResolver("dummy")).isNull();
        assertThat(notif.getResolver("fileCreated")) //
                                                    .isNotNull();
        assertThat(notif.getResolver("fileUpdated")) //
                                                    .isNotNull();
        assertThat(notif.getResolvers()).hasSize(3);
        assertThat(notif.getEventTransformer("unknown")).isNull();
        assertThat(notif.getEventTransformer("basicTransformer")).isNotNull();
        assertThat(notif.getEventTransformers()).hasSize(2);
    }

    @Test
    public void testEventTransformer() {
        EventContext eventContext = new EventContextImpl();
        eventContext.getProperties().put(KEY_INFO, "Test Info");
        eventContext.setRepositoryName("test");
        Event event = new EventImpl(DOCUMENT_UPDATED, eventContext);

        EventTransformer transformer = notif.getEventTransformer("anotherBasicTransformer");
        assertThat(transformer.getId()).isEqualTo("anotherBasicTransformer");
        assertThat(transformer.accept(event)).isTrue();
        assertThat(transformer.buildEventRecordContext(event).get(KEY_INFO)).isEqualTo("Test Info");

        event = new EventImpl(DOCUMENT_CREATED, eventContext);
        assertThat(transformer.accept(event)).isFalse();

        Map<String, String> ctx = notif.getEventTransformer("basicTransformer").buildEventRecordContext(event);
        assertThat(ctx).hasSize(2);
        assertThat(ctx.get(KEY_EVENT_NAME)).isEqualTo(DOCUMENT_CREATED);
        assertThat(ctx.get("basicTransformer")).isEqualTo("Transformer Name");
    }

    @Test
    public void testNotifierInit() {
        Notifier log = notif.getNotifier("log");
        assertThat(log.getProperty("dummy", "john")).isEqualTo("john");
        assertThat(log.getProperty("dummy")).isNull();
        assertThat(log.getProperty("my-secret-key")).isEqualTo("my-secret-value");
    }

    @Test
    public void testResolverResolution() {
        EventRecordBuilder builder = EventRecord.builder()
                                                .withEventName(DOCUMENT_UPDATED)
                                                .withUsername(session.getPrincipal().getName())
                                                .withDocumentId("0000-0000-0000")
                                                .withDocumentType("File");

        assertThat(notif.getResolvers(builder.build())).hasSize(1).first().isInstanceOf(FileUpdatedResolver.class);

        builder.withEventName(DOCUMENT_CREATED);
        assertThat(notif.getResolvers(builder.build())).hasSize(1).first().isInstanceOf(FileCreatedResolver.class);

        builder.withEventName(DOCUMENT_CHECKEDIN);
        assertThat(notif.getResolvers(builder.build())).hasSize(0);
    }

    @Test(expected = NuxeoException.class)
    public void testMissingSubscriptionsKeys() {
        Map<String, String> ctx = Collections.emptyMap();
        notif.getSubscriptions("complexKey", ctx);
    }

    @Test
    public void testMissingSubscriptionsKeysMessage() {
        try {
            Map<String, String> ctx = new HashMap<>();
            ctx.put(ComplexSubsKeyResolver.NAME_FIELD, "myName");
            notif.getSubscriptions("complexKey", ctx);

            fail("Should have thrown a NuxeoException.");
        } catch (NuxeoException ne) {
            assertThat(ne.getMessage()).isEqualTo(
                    "Missing required context fields: " + ComplexSubsKeyResolver.SUFFIX_FIELD);
        }
    }

    @Test
    public void testSubscriptionsStorage() {
        String resolverId = "complexKey";
        Map<String, String> ctx = new HashMap<>();
        ctx.put(ComplexSubsKeyResolver.NAME_FIELD, "aName");
        ctx.put(ComplexSubsKeyResolver.SUFFIX_FIELD, "aSuffix");

        String firstUser = "dummy";
        String secondUser = "Johnny";
        String thirdUser = "Arthur";

        Subscribers subs = notif.getSubscriptions(resolverId, ctx);

        assertThat(subs).isNotNull();

        scb.doSubscribe(firstUser, resolverId, ctx);
        subs = notif.getSubscriptions(resolverId, ctx);

        assertThat(subs).isNotNull();
        assertThat(subs.getUsernames()).containsExactly(firstUser);

        scb.doSubscribe(secondUser, resolverId, ctx);
        subs = notif.getSubscriptions(resolverId, ctx);
        assertThat(subs.getUsernames()).containsExactly(firstUser, secondUser);

        // Change a ctx key to ensure we have different results
        ctx.put(ComplexSubsKeyResolver.NAME_FIELD, "newName");
        scb.doSubscribe(thirdUser, resolverId, ctx);
        subs = notif.getSubscriptions(resolverId, ctx);
        assertThat(subs.getUsernames()).containsExactly(thirdUser);
    }

    @Test
    public void testResolverSubscriptions() {
        String resolverId = "fileCreated";
        Map<String, String> ctx = Collections.emptyMap();
        EventRecord emptyRecord = EventRecord.builder().build();

        Resolver resolver = notif.getResolver(resolverId);
        assertThat(resolver.resolveTargetUsers(emptyRecord)).isEmpty();

        String username = "toto";
        scb.doSubscribe(username, resolverId, ctx);
        assertThat(resolver.resolveTargetUsers(emptyRecord)).isNotEmpty().containsExactly(username);
    }

    @Test
    public void testResolverUnsubscribe() {
        String resolverId = "complexKey";
        Map<String, String> ctx = new HashMap<>();
        ctx.put(ComplexSubsKeyResolver.NAME_FIELD, "aName");
        ctx.put(ComplexSubsKeyResolver.SUFFIX_FIELD, "aSuffix");

        String firstUser = "dummy";

        scb.doSubscribe(firstUser, resolverId, ctx);
        Subscribers subs = notif.getSubscriptions(resolverId, ctx);

        assertThat(subs.getUsernames()).hasSize(1);

        scb.doUnsubscribe(firstUser, resolverId, ctx);
        subs = notif.getSubscriptions(resolverId, ctx);
        assertThat(subs.getUsernames()).isEmpty();
    }

    @Test(expected = NuxeoException.class)
    public void testSubscriptionForMissingResolver() {
        notif.getSubscriptions("somethiiiing", null);
        failBecauseExceptionWasNotThrown(NuxeoException.class);
    }

    @Test
    public void testNotificationSettings() {
        NotificationComponent cmp = (NotificationComponent) notif;
        SettingsDescriptor.NotifierSetting setting = cmp.getSetting("fileCreated").getSetting("inApp");
        assertThat(setting.isDefault()).isTrue();
        assertThat(setting.isEnabled()).isTrue();

        setting = cmp.getSetting("fileCreated").getSetting("unknown");
        assertThat(setting.isDefault()).isFalse();
        assertThat(setting.isEnabled()).isTrue();

        setting = cmp.getSetting("fileUpdated").getSetting("log");
        assertThat(setting.isEnabled()).isFalse();
    }

    @Test
    public void testContextInNotifier() {
        DocumentModel doc = mock(DocumentModel.class);
        when(doc.getId()).thenReturn("0000-0000-0000");
        when(doc.getRepositoryName()).thenReturn("repo-test");
        when(doc.getType()).thenReturn("File");

        Resolver resolver = mock(Resolver.class);
        when(resolver.getId()).thenReturn("klaxon");

        EventRecord eventRecord = EventRecord.builder()
                                             .withEventName("test")
                                             .withUsername("Administrator")
                                             .withDocument(doc)
                                             .build();

        assertThat(eventRecord.getDocumentSourceId()).isEqualTo("0000-0000-0000");

        Notification notif = Notification.builder()
                                         .fromEvent(eventRecord)
                                         .withUsername("bobby")
                                         .withResolver(resolver)
                                         .build();

        assertThat(notif.getSourceId()).isEqualTo("0000-0000-0000");
        assertThat(notif.getSourceRepository()).isEqualTo("repo-test");
        assertThat(notif.getContext()).isNotEmpty()
                                      .containsEntry(ORIGINATING_EVENT, "test")
                                      .containsEntry(ORIGINATING_USER, "Administrator");
    }
}
