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

package org.nuxeo.ecm.platform.notification.resolver;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;

/**
 * Resolver class aims to be able to transform a CoreEvent to a Notification object
 *
 * @since XXX
 */
public abstract class Resolver {

    protected int order;

    /**
     * Check if the resolver is able to compute the current Event in a Notification Object.
     * <p>
     * The method is executed on EVERY core event, his resolution MUST be as fast as possible.
     * 
     * @param event that could be transformed.
     * @return true if it assignable, false otherwise.
     */
    public abstract boolean accept(Event event);

    /**
     * Resolve target users that have to be notified
     * <p>
     * The method is executed on EVERY corresponding notification, his resolution MUST be as fast as possible.
     *
     * @param doc from the context that contain resolution needs
     * @return list of target users, of an empty list otherwise.
     */
    public abstract List<String> resolveTargetUsers(DocumentModel doc);

    /**
     * Allow to order several resolvers when matching the same event
     * 
     * @return order value
     */
    public int getOrder() {
        return order;
    }

    /**
     * Initialize Resolver
     * 
     * @param desc tied to the Resolver
     * @return the current instance.
     */
    protected Resolver init(ResolverDescriptor desc) {
        order = desc.getOrder();
        return this;
    }
}
