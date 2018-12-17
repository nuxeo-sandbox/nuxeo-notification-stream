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

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.notification.resolver.SubscribableResolver;
import org.nuxeo.ecm.restapi.server.jaxrs.notification.AbstractNotificationObject;
import org.nuxeo.ecm.webengine.model.WebObject;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NOT_MODIFIED;

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
    public Object getResolver() {
        return resolver;
    }

    @POST
    @Path("/subscribe")
    @Consumes(APPLICATION_JSON)
    public Response subscribe(String data) {
        String username = getUsername();
        Map<String, String> ctx = readJson(data);

        checkContext(ctx);
        if (getNotifService().hasSubscribe(username, resolver.getId(), ctx)) {
            return Response.status(NOT_MODIFIED).build();
        }

        getNotifService().subscribe(username, resolver.getId(), ctx);
        return Response.status(CREATED).build();
    }

    @POST
    @Path("/unsubscribe")
    @Consumes(APPLICATION_JSON)
    public Response unsubscribe(String data) {
        return doUnsubscribe(readJson(data));
    }

    @GET
    @Path("/unsubscribe")
    public Response unsubscribe() {
        Map<String, String> notifCtx = new HashMap<>();
        SubscribableResolver resolver = asSubscribable();
        resolver.getRequiredContextFields().forEach(k -> notifCtx.put(k, ctx.getRequest().getParameter(k)));

        return doUnsubscribe(notifCtx);
    }

    protected Response doUnsubscribe(Map<String, String> ctx) {
        SubscribableResolver resolver = asSubscribable();

        String username = getUsername();
        checkContext(ctx);
        if (!getNotifService().hasSubscribe(username, resolver.getId(), ctx)) {
            return Response.status(NOT_FOUND).build();
        }

        getNotifService().unsubscribe(username, resolver.getId(), ctx);
        return Response.status(ACCEPTED).build();
    }

    protected void checkContext(Map<String, String> ctx) {
        SubscribableResolver resolver = asSubscribable();
        if (!resolver.hasRequiredFields(ctx)) {
            throw new NuxeoException("Missing fields to handle registrations: " + StringUtils.join(resolver.getMissingFields(ctx), ", "), BAD_REQUEST.getStatusCode());
        }
    }

    protected SubscribableResolver asSubscribable() {
        if (!(resolver instanceof SubscribableResolver)) {
            throw new NuxeoException("This resolver is not able to handle subscriptions.", BAD_REQUEST.getStatusCode());
        }

        return (SubscribableResolver) resolver;
    }
}
