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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoException;

public class AbstractNotificationObjectTest {

    @Test
    public void readJsonWithObject() throws IOException {
        Map<String, Object> obj = new HashMap<>();
        obj.put("key1", "value1");
        obj.put("key2", Collections.singletonMap("child", 1));
        obj.put("key3", 1);
        obj.put("key4", Arrays.asList("1", "2"));

        String json = new ObjectMapper().writeValueAsString(obj);
        Map<String, String> ctx = AbstractNotificationObject.readJson(json);
        assertThat(ctx.containsKey("key1")).isTrue();
        assertThat(ctx.containsKey("key3")).isTrue();
        assertThat(ctx.get("key3")).isEqualTo("1");

        assertThat(ctx.containsKey("key2")).isFalse();
        assertThat(ctx.containsKey("key4")).isFalse();
    }

    @Test
    public void testBadJson() {
        String badJson = "{dasdsad,}";
        try {
            AbstractNotificationObject.readJson(badJson);
            fail("Should have thrown an exception");
        } catch (NuxeoException e) {
            assertThat(e.getStatusCode()).isEqualTo(BAD_REQUEST.getStatusCode());
        }
    }
}