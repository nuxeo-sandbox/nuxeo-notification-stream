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
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.restapi.server.jaxrs.notification.AbstractNotificationObject;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @since XXX
 */
@WebObject(type = ResolverObject.TYPE)
public class ResolverObject extends AbstractNotificationObject {

    public static final String TYPE = "notification-resolver";

    @GET
    @Path("/")
    public List<Resolver> getResolvers() {
        return new ArrayList<>(getNotifService().getResolvers());
    }

    @GET
    @Path("/{resolverId}")
    public Object getResolver(@PathParam("resolverId") String resolverId) {
        Resolver resolver = getNotifService().getResolver(resolverId);
        return resolver != null ? resolver : Response.status(NOT_FOUND).build();
    }

    @POST
    @Path("/{resolverId}")
    public Response subscribe(@PathParam("resolverId") String resolverId) {
        String username = getUsername();

        if (getNotifService().hasSubscribe(username, resolverId, computeContext())) {
            return Response.status(NOT_MODIFIED).build();
        }

        getNotifService().subscribe(username, resolverId, computeContext());
        return Response.status(CREATED).build();
    }

    @DELETE
    @Path("/{resolverId}")
    public Response unsubscribe(@PathParam("resolverId") String resolverId) {
        String username = getUsername();

        if (!getNotifService().hasSubscribe(username, resolverId, computeContext())) {
            return Response.status(NOT_FOUND).build();
        }

        getNotifService().unsubscribe(username, resolverId, computeContext());
        return Response.status(ACCEPTED).build();
    }

    protected Map<String, String> computeContext() {
        return Collections.emptyMap();
    }
}
