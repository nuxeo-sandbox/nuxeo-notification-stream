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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.notification.notifier.Notifier;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.restapi.server.jaxrs.notification.webobject.NotifierObject;
import org.nuxeo.ecm.restapi.server.jaxrs.notification.webobject.ResolverObject;
import org.nuxeo.ecm.restapi.server.jaxrs.notification.webobject.SettingsObject;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @since XXX
 */
@WebObject(type = "notification")
@Produces(APPLICATION_JSON)
public class NotificationRoot extends AbstractNotificationObject {

    @GET
    @Path("/resolver")
    public List<Resolver> getResolvers() {
        return new ArrayList<>(getNotifService().getResolvers());
    }

    @GET
    @Path("/notifier")
    public List<Notifier> getNotifiers() {
        return new ArrayList<>(getNotifService().getNotifiers());
    }

    @Path("resolver/{resolverId}")
    public Object toResolverObject(@PathParam("resolverId") String resolverId) {
        Resolver resolver = getNotifService().getResolver(resolverId);
        if (resolver == null) {
            throw new NuxeoException(NOT_FOUND.getStatusCode());
        }
        return newObject(ResolverObject.TYPE, resolver);
    }

    @Path("/notifier/{notifierId}")
    public Object toNotifierObject(@PathParam("notifierId") String notifierId) {
        Notifier notifier = getNotifService().getNotifier(notifierId);
        if (notifier == null) {
            throw new NuxeoException(NOT_FOUND.getStatusCode());
        }
        return newObject(NotifierObject.TYPE, notifier);
    }

    @Path("settings")
    public Object toSettingsObject() {
        return newObject(SettingsObject.TYPE);
    }
}
