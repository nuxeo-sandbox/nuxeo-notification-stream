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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.NotificationSettingsService;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 0.1
 */
public abstract class AbstractNotificationObject extends AbstractResource<ResourceTypeImpl> {

    protected NotificationService getNotifService() {
        return Framework.getService(NotificationService.class);
    }

    protected NotificationSettingsService getSettingsService() {
        return Framework.getService(NotificationSettingsService.class);
    }

    protected String getUsername() {
        return getContext().getPrincipal().getName();
    }

    protected static Map<String, String> readJson(String json) {
        try {
            Map<String, String> params = new HashMap<>();
            JsonNode jsonTree = new ObjectMapper().readTree(json);
            jsonTree.getFields().forEachRemaining((entry) -> {
                JsonNode value = entry.getValue();
                if (value.isValueNode()) {
                    params.put(entry.getKey(), value.getValueAsText());
                }
            });
            return params;
        } catch (IOException e) {
            throw new NuxeoException(BAD_REQUEST.getStatusCode());
        }
    }
}
