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

package org.nuxeo.ecm.restapi.server.jaxrs.notification;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.NotificationSettingsService;
import org.nuxeo.ecm.notification.model.UserNotifierSettings;
import org.nuxeo.ecm.notification.io.NotifierListJsonWriter;
import org.nuxeo.ecm.notification.io.UserNotifierSettingsJsonWriter;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.stream.StreamHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.JsonNode;

@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, NotificationFeature.class })
@ServletContainer(port = 18090)
@Deploy("org.nuxeo.ecm.platform.notification.stream.rest")
@Deploy("org.nuxeo.ecm.platform.notification.stream.rest.test")
public class UserSettingsObjectTest extends BaseTest {

    @Inject
    NotificationSettingsService nss;

    @Test
    public void testGetSettings() throws IOException {
        JsonNode json = getResponseAsJson(RequestType.GET, "/notification/settings");
        assertThat(json).isNotNull();
        assertThat(json.get("fileCreated").get("log").booleanValue()).isFalse();
        assertThat(json.get("fileCreated").get("inApp").booleanValue()).isFalse();
    }

    @Test
    public void getResolverSettings() throws IOException {
        try (CloseableClientResponse res = getResponse(RequestType.GET, "/notification/settings/missing")) {
            assertThat(res.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        }

        JsonNode json = getResponseAsJson(RequestType.GET, "/notification/settings/fileCreated");
        assertThat(json.get("log").booleanValue()).isFalse();
        assertThat(json.get("inApp").booleanValue()).isFalse();
    }

    @Test
    public void testUpdateSettings() throws IOException {
        assertThat(nss.getSelectedNotifiers("Administrator", "fileCreated")).isEmpty();

        try (CloseableClientResponse res = getResponse(RequestType.PUT, "/notification/settings/fileCreated",
                "{\"log\": true, \"entity-type\": \"" + UserNotifierSettingsJsonWriter.ENTITY_TYPE + "\"}")) {
            assertThat(res.getStatus()).isEqualTo(ACCEPTED.getStatusCode());
        }

        StreamHelper.drainAndStop();

        UserNotifierSettings settings = nss.getResolverSettings("Administrator").getSettings("fileCreated");
        assertThat(settings.getSelectedNotifiers()).hasSize(1).contains("log");

        JsonNode json = getResponseAsJson(RequestType.GET, "/notification/settings/fileCreated/selected");
        assertThat(json.get("entity-type").asText()).isEqualTo(NotifierListJsonWriter.ENTITY_TYPE);
        assertThat(json.get("entries").isArray()).isTrue();
        assertThat(json.get("entries").get(0).get("name").asText()).isEqualTo("log");
    }
}
