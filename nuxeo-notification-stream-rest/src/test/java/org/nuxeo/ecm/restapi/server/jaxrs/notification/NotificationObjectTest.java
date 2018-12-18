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

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.io.NotifierJsonWriter;
import org.nuxeo.ecm.notification.io.NotifierListJsonWriter;
import org.nuxeo.ecm.notification.io.ResolverJsonWriter;
import org.nuxeo.ecm.notification.io.ResolverListJsonWriter;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.stream.StreamHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.nuxeo.ecm.notification.resolver.ComplexSubsKeyResolver.NAME_FIELD;
import static org.nuxeo.ecm.notification.resolver.ComplexSubsKeyResolver.SUFFIX_FIELD;

@RunWith(FeaturesRunner.class)
@Features({RestServerFeature.class, NotificationFeature.class})
@ServletContainer(port = 18090)
@Deploy("org.nuxeo.ecm.platform.notification.stream.rest")
@Deploy("org.nuxeo.ecm.platform.notification.stream.rest.test")
public class NotificationObjectTest extends BaseTest {

    @Test
    public void testUnknownResolver() {
        try (CloseableClientResponse res = getResponse(RequestType.GET, "/notification/resolver/missing")) {
            assertThat(res.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void testResolverWriter() throws IOException {
        JsonNode json = getResponseAsJson(RequestType.GET, "/notification/resolver/fileIsCreated");
        assertThat(json.get("entity-type").asText()).isEqualTo(ResolverJsonWriter.ENTITY_TYPE);
        assertThat(json.get("id").asText()).isEqualTo("fileIsCreated");
        assertThat(json.get("message").asText()).isEqualTo("Hello World!");
    }

    @Test
    public void testSubscribableResolverWriter() throws IOException {
        JsonNode json = getResponseAsJson(RequestType.GET, "/notification/resolver/complexResolver");
        assertThat(json.get("entity-type").asText()).isEqualTo(ResolverJsonWriter.ENTITY_TYPE);
        assertThat(json.get("id").asText()).isEqualTo("complexResolver");
        assertThat(json.get("label").asText()).isEqualTo("Complex Resolver");
        assertThat(json.get("description").asText()).isEqualTo("A really complex resolver.");
        assertThat(json.get("requiredFields").isArray()).isTrue();

        List<String> requiredFields = new ArrayList<>();
        json.get("requiredFields").elements().forEachRemaining(j -> requiredFields.add(j.asText()));
        assertThat(requiredFields).hasSize(2).containsExactlyInAnyOrder(NAME_FIELD, SUFFIX_FIELD);
    }

    @Test
    public void testResolverListWriter() throws IOException {
        JsonNode json = getResponseAsJson(RequestType.GET, "/notification/resolver");
        assertThat(json.get("entity-type").asText()).isEqualTo(ResolverListJsonWriter.ENTITY_TYPE);
        assertThat(json.get("entries").isArray()).isTrue();
    }

    @Test
    public void testUnknonNotifier() {
        try (CloseableClientResponse res = getResponse(RequestType.GET, "/notification/notifier/missing")) {
            assertThat(res.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void testNotifierWriter() throws IOException {
        JsonNode json = getResponseAsJson(RequestType.GET, "/notification/notifier/log");
        assertThat(json.get("entity-type").asText()).isEqualTo(NotifierJsonWriter.ENTITY_TYPE);
        assertThat(json.get("name").asText()).isEqualTo("log");
        assertThat(json.get("label").asText()).isEqualTo("Log Notifier");
        assertThat(json.get("description").asText()).isEqualTo("Display notifications as logs.");
    }

    @Test
    public void testNotifierListWriter() throws IOException {
        JsonNode json = getResponseAsJson(RequestType.GET, "/notification/notifier");
        assertThat(json.get("entity-type").asText()).isEqualTo(NotifierListJsonWriter.ENTITY_TYPE);
        assertThat(json.get("entries").isArray()).isTrue();
    }

    @Test
    public void testSubscribeAndUnsubscribe() {
        try (CloseableClientResponse res = getResponse(RequestType.POST, "/notification/resolver/fileIsCreated/subscribe",
                "{}")) {
            assertThat(res.getStatus()).isEqualTo(CREATED.getStatusCode());
        }

        // Wait until computations are processed
        assertThat(StreamHelper.drainAndStop()).isTrue();

        // Second call should not modify existing registration
        try (CloseableClientResponse res = getResponse(RequestType.POST, "/notification/resolver/fileIsCreated/subscribe",
                "{}")) {
            assertThat(res.getStatus()).isEqualTo(NOT_MODIFIED.getStatusCode());
        }

        try (CloseableClientResponse res = getResponse(RequestType.POST,
                "/notification/resolver/fileIsCreated/unsubscribe", "{}")) {
            assertThat(res.getStatus()).isEqualTo(ACCEPTED.getStatusCode());
        }

        // Wait until computations are processed
        assertThat(StreamHelper.drainAndStop()).isTrue();

        // Second call should response not found
        try (CloseableClientResponse res = getResponse(RequestType.POST,
                "/notification/resolver/fileIsCreated/unsubscribe", "{}")) {
            assertThat(res.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void testSubscribeWithException() {
        try (CloseableClientResponse res = getResponse(RequestType.POST, "/notification/resolver/complexResolver/subscribe",
                "{}")) {
            assertThat(res.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void testUnsubscribeWithException() {
        try (CloseableClientResponse res = getResponse(RequestType.POST,
                "/notification/resolver/complexResolver/unsubscribe", "{}")) {
            assertThat(res.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void testUnsubscribeWithGet() {
        try (CloseableClientResponse res = getResponse(RequestType.POST, "/notification/resolver/complexResolver/subscribe",
                "{\"name\":\"Waldo\",\"suffix\":\"Doh\"}")) {
            assertThat(res.getStatus()).isEqualTo(CREATED.getStatusCode());
        }

        // Wait until computations are processed
        assertThat(StreamHelper.drainAndStop()).isTrue();

        MultivaluedMap<String, String> qs = new MultivaluedMapImpl();
        qs.add("name", "Waldo");

        try (CloseableClientResponse res = getResponse(RequestType.GET,
                "/notification/resolver/complexResolver/unsubscribe", qs)) {
            assertThat(res.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
        }

        qs.add("suffix", "Doh");
        try (CloseableClientResponse res = getResponse(RequestType.GET,
                "/notification/resolver/complexResolver/unsubscribe", qs)) {
            assertThat(res.getStatus()).isEqualTo(ACCEPTED.getStatusCode());
        }

        assertThat(StreamHelper.drainAndStop()).isTrue();
    }
}