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
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.notifier.Notifier;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since XXX
 */
@WebObject(type = "notification")
@Produces(APPLICATION_JSON)
public class NotificationObject extends AbstractResource<ResourceTypeImpl> {

    @GET
    @Path("resolver")
    public List<Resolver> getResolvers() {
        return new ArrayList<>(getService().getResolvers());
    }

    @GET
    @Path("resolver/{resolverId}")
    public Object getResolver(@PathParam("resolverId") String resolverId) {
        Resolver resolver = getService().getResolver(resolverId);
        return resolver != null ? resolver : Response.status(NOT_FOUND).build();
    }

    @POST
    @Path("resolver/{resolverId}")
    public Response subscribe(@PathParam("resolverId") String resolverId) {
        String username = getContext().getPrincipal().getName();

        if (getService().hasSubscribe(username, resolverId, computeContext())) {
            return Response.status(NOT_MODIFIED).build();
        }

        getService().subscribe(username, resolverId, computeContext());
        return Response.status(CREATED).build();
    }

    @DELETE
    @Path("resolver/{resolverId}")
    public Response unsubscribe(@PathParam("resolverId") String resolverId) {
        String username = getContext().getPrincipal().getName();

        if (!getService().hasSubscribe(username, resolverId, computeContext())) {
            return Response.status(NOT_FOUND).build();
        }

        getService().unsubscribe(username, resolverId, computeContext());
        return Response.status(ACCEPTED).build();
    }

    @GET
    @Path("notifier")
    public List<Notifier> getNotifiers() {
        return new ArrayList<>(getService().getNotifiers());
    }

    @GET
    @Path("notifier/{notifierId}")
    public Object getNotifier(@PathParam("notifierId") String notifierId) {
        Notifier notifier = getService().getNotifier(notifierId);
        return notifier != null ? notifier : Response.status(NOT_FOUND).build();
    }

    protected Map<String, String> computeContext() {
        return Collections.emptyMap();
    }

    protected NotificationService getService() {
        return Framework.getService(NotificationService.class);
    }
}
