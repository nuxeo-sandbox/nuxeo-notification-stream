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

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.NotificationSettingsService;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since XXX
 */
public abstract class AbstractNotificationObject extends AbstractResource<ResourceTypeImpl> {
    private static final Logger log = LogManager.getLogger();

    protected NotificationService getNotifService() {
        return Framework.getService(NotificationService.class);
    }

    protected NotificationSettingsService getSettingsService() {
        return Framework.getService(NotificationSettingsService.class);
    }

    protected String getUsername() {
        return getContext().getPrincipal().getName();
    }

    protected Response buildResponse(Response.Status status, Object obj) {
        try {
            String message = new ObjectMapper().writeValueAsString(obj);

            return Response.status(status)
                           .header("Content-Length", message.getBytes(UTF_8).length)
                           .type(MediaType.APPLICATION_JSON + "; charset=" + UTF_8)
                           .entity(message)
                           .build();
        } catch (JsonProcessingException e) {
            log.warn("Unable to map object to JSON", e);
            return Response.serverError().build();
        }
    }
}
