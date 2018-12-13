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

package org.nuxeo.ecm.notification.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.resolver.Resolver;

public class TestResolverMessageReplacer {
    @Test
    public void testBasicString() {
        Map<String, String> ctx = Collections.singletonMap("username", "Jack");
        String message = ResolverMessageReplacer.from("Hello @{username}!", ctx).replace();
        assertThat(message).isEqualTo("Hello Jack!");
    }

    @Test
    public void testWithKey() {
        Map<String, String> ctx = Collections.singletonMap("key", "value");
        String message = ResolverMessageReplacer.from("Hello @{user:key}!", ctx).replace();
        assertThat(message).isEqualTo("Hello @{user:value}!");
    }

    @Test
    public void testWithMultipleValues() {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("repository", "default");
        ctx.put("docId", "0000-000-00-00-000");
        String message = ResolverMessageReplacer.from("Document @{doc:repository:docId} modified!", ctx).replace();
        assertThat(message).isEqualTo("Document @{doc:default:0000-000-00-00-000} modified!");
    }

    @Test
    public void testNotificationBuilder() {
        EventRecord eventRecord = EventRecord.builder()
                                             .withTime(5)
                                             .withDocumentId("0000-0000")
                                             .withDocumentRepository("test")
                                             .withUsername("johndoe")
                                             .build();

        Resolver resolver = mock(Resolver.class);
        when(resolver.getMessage()).thenReturn(
                "Doc @{doc:sourceRepository:sourceId} updated by @{user:originatingUser} at @{date:createdAt}");

        Notification notif = Notification.builder() //
                                         .fromEvent(eventRecord)
                                         .withResolver(resolver)
                                         .build();

        assertThat(notif.getMessage()).isEqualTo("Doc @{doc:test:0000-0000} updated by @{user:johndoe} at @{date:5}");
    }
}
