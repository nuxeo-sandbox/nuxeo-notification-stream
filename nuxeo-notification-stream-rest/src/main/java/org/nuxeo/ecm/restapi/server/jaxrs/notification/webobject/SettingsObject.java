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

package org.nuxeo.ecm.restapi.server.jaxrs.notification.webobject;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.notification.model.UserNotifierSettings;
import org.nuxeo.ecm.notification.notifier.Notifier;
import org.nuxeo.ecm.restapi.server.jaxrs.notification.AbstractNotificationObject;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @since 0.1
 */
@WebObject(type = SettingsObject.TYPE)
@Produces(APPLICATION_JSON)
public class SettingsObject extends AbstractNotificationObject {

    public static final String TYPE = "notification-settings";

    @GET
    @Path("/")
    public Object getUserSettings() {
        return getSettingsService().getResolverSettings(getUsername());
    }

    @GET
    @Path("/{resolverId}")
    public Object getResolverSettings(@PathParam("resolverId") String resolverId) {
        UserNotifierSettings uns = getSettingsService().getResolverSettings(getUsername()).getSettings(resolverId);
        return uns != null ? uns : Response.status(NOT_FOUND).build();
    }

    @PUT
    @Path("/{resolverId}")
    @Consumes(APPLICATION_JSON)
    public Response updateResolverSettings(@PathParam("resolverId") String resolverId, UserNotifierSettings nus) {
        if (getNotifService().getResolver(resolverId) == null) {
            return Response.status(NOT_FOUND).build();
        }

        getSettingsService().updateSettings(getUsername(), Collections.singletonMap(resolverId, nus));

        return Response.status(ACCEPTED).build();
    }

    @GET
    @Path("/{resolverId}/selected")
    public List<Notifier> getSelectedNotifiers(@PathParam("resolverId") String resolverId) {
        return getSettingsService().getSelectedNotifiers(getUsername(), resolverId);
    }
}
