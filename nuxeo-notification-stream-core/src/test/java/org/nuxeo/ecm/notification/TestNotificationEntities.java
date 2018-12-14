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
import static org.nuxeo.ecm.notification.entities.TextEntity.USERNAME;
import static org.nuxeo.ecm.notification.message.Notification.ORIGINATING_USER;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.notification.entities.TextEntity;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.message.Notification.NotificationBuilder;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ NotificationFeature.class, PlatformFeature.class })
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.url.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/basic-contrib.xml")
public class TestNotificationEntities {

    @Inject
    protected NotificationService ns;

    @Inject
    protected CoreSession session;

    @Test
    public void testTextSupplierWithUser() {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("primary", "Administrator");
        ctx.put("other", "Administrator");

        String message = "Hello @{user:primary}! @{user:other} pings you!";

        NotificationBuilder builder = Notification.builder()
                                                  .withCtx(ctx)
                                                  .withResolverMessage(message)
                                                  .computeMessage()
                                                  .prepareEntities();

        Notification notif = builder.build();
        assertThat(notif.getMessage()).isEqualTo("Hello @{user:Administrator}! @{user:Administrator} pings you!");
        assertThat(notif.getEntities()).hasSize(2);
        assertThat(notif.getEntities()).anySatisfy(TextEntity::isComputed);

        TextEntity entity = notif.getEntities().get(0);
        assertThat(entity.getType()).isEqualTo(USERNAME);
        assertThat(entity.getId()).isEqualTo("Administrator");
        assertThat(entity.getStart()).isEqualTo(6);
        assertThat(entity.getEnd()).isEqualTo(27);

        notif = builder.resolveEntities().build();
        assertThat(notif.getEntities()).hasSize(2);
        assertThat(notif.getEntities()).allMatch(TextEntity::isComputed);

        entity = notif.getEntities().get(0);
        assertThat(entity.getValue("email")).isEqualTo("devnull@nuxeo.com");
    }

    @Test
    public void testNotificationMessageComputed() {
        DocumentModel doc = session.createDocumentModel("/", "fooBar", "File");
        doc.setPropertyValue("dc:title", "Hello My Title");
        doc = session.createDocument(doc);
        session.save();

        Map<String, String> ctx = new HashMap<>();
        ctx.put("author", "Administrator");
        ctx.put("myFile", doc.getId());

        String message = "Hello @{user:author}! @{doc:myFile} pings you!";

        Notification notif = Notification.builder()
                                         .withCtx(ctx)
                                         .withCtx(ORIGINATING_USER, "Administrator")
                                         .withSourceRepository(doc.getRepositoryName())
                                         .withResolverMessage(message)
                                         .computeMessage()
                                         .prepareEntities()
                                         .resolveEntities()
                                         .build();

        assertThat(notif.getEntities()).hasSize(2);
        assertThat(notif.getEntities()).allMatch(TextEntity::isComputed);

        TextEntity entity = notif.getEntities().get(0);
        assertThat(entity.getValue("email")).isEqualTo("devnull@nuxeo.com");

        entity = notif.getEntities().get(1);
        assertThat(entity.getId()).isEqualTo(doc.getId());
        assertThat(entity.getValue("title")).isEqualTo("Hello My Title");
    }
}
