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
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.platform.notification.resolver.DynDocumentResolver.DOC_ID_KEY;
import static org.nuxeo.ecm.platform.notification.resolver.DynDocumentResolver.EVENT_KEY;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.ecm.platform.notification.message.EventRecord.EventRecordBuilder;
import org.nuxeo.ecm.platform.notification.resolver.Resolver;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/complex-resolvers-contrib.xml")
public class TestComplexResolvers {

    @Inject
    protected NotificationService ns;

    @Inject
    protected NotificationStreamCallback nsc;

    @Inject
    protected CoreSession session;

    @Test
    public void testContributorsResolver() {
        DocumentModel file = session.createDocumentModel("/", RandomStringUtils.randomAlphabetic(5), "File");
        file.setPropertyValue("dc:contributors", (Serializable) Arrays.asList("Administrator", "system", "dummy"));
        file = session.createDocument(file);

        Resolver resolver = ns.getResolver("contributorsResolver");
        assertThat(resolver).isNotNull();

        EventRecord record = EventRecord.builder()
                                        .withDocument(file)
                                        .withEventName(DOCUMENT_UPDATED)
                                        .withUsername("Administrator")
                                        .build();

        assertThat(resolver.accept(record)).isTrue();
        assertThat(resolver.resolveTargetUsers(record)).hasSize(3)
                                                       .containsExactlyInAnyOrder("Administrator", "system", "dummy");
    }

    @Test
    public void testWithContextSubscriptions() {
        DocumentModel file = session.createDocumentModel("/", RandomStringUtils.randomAlphabetic(5), "File");
        file = session.createDocument(file);

        Resolver resolver = ns.getResolver("documentResolver");
        assertThat(resolver).isNotNull();

        EventRecordBuilder builder = EventRecord.builder()
                                                .withDocument(file)
                                                .withEventName(DOCUMENT_CREATED)
                                                .withUsername("someone");

        assertThat(resolver.resolveTargetUsers(builder.build())).hasSize(0);

        Map<String, String> ctx = new HashMap<>();
        ctx.put(DOC_ID_KEY, file.getId());
        ctx.put(EVENT_KEY, DOCUMENT_CREATED);
        nsc.doSubscribe("Administrator", resolver.getId(), ctx);

        builder.withEventName(DOCUMENT_UPDATED);
        assertThat(resolver.resolveTargetUsers(builder.build())).isEmpty();

        builder.withEventName(DOCUMENT_CREATED);
        builder.withDocumentId("000-000-000");
        assertThat(resolver.resolveTargetUsers(builder.build())).isEmpty();
    }
}
