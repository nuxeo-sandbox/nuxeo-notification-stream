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
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.restapi.server.jaxrs.notification.AbstractNotificationObject;
import org.nuxeo.ecm.webengine.model.WebObject;

/**
 * @since XXX
 */
@WebObject(type = ResolverObject.TYPE)
@Produces(APPLICATION_JSON)
public class ResolverObject extends AbstractNotificationObject {

    public static final String TYPE = "notification-resolver";

    protected Resolver resolver;

    @Override
    protected void initialize(Object... args) {
        resolver = (Resolver) args[0];
    }

    @GET
    @Path("/")
    public Object setResolver() {
        return resolver;
    }

    @POST
    @Path("/subscribe")
    @Consumes(APPLICATION_JSON)
    public Response subscribe() {
        String username = getUsername();

        if (getNotifService().hasSubscribe(username, resolver.getId(), computeContext())) {
            return Response.status(NOT_MODIFIED).build();
        }

        getNotifService().subscribe(username, resolver.getId(), computeContext());
        return Response.status(CREATED).build();
    }

    @POST
    @Path("/unsubscribe")
    @Consumes(APPLICATION_JSON)
    public Response unsubscribe() {
        String username = getUsername();

        if (!getNotifService().hasSubscribe(username, resolver.getId(), computeContext())) {
            return Response.status(NOT_FOUND).build();
        }

        getNotifService().unsubscribe(username, resolver.getId(), computeContext());
        return Response.status(ACCEPTED).build();
    }

    protected Map<String, String> computeContext() {
        return Collections.emptyMap();
    }
}
