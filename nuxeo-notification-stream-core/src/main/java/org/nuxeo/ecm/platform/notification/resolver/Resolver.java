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
import java.util.Map;

import org.nuxeo.ecm.core.event.Event;

/**
 * Resolver class aims to be able to transform a CoreEvent to a Notification object
 *
 * @since XXX
 */
public abstract class Resolver {

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
     * @param event from the context that contain resolution needed
     * @return list of target users, of an empty list otherwise.
     */
    public abstract List<String> resolveTargetUsers(Event event);

    /**
     * Subscribe the given user to the resolver. This allows to resolve the target users whenever an event accepted by the resolver is triggered and need to be processed.
     *
     * @param username The username subscribing to the resolver.
     * @param ctx      A map of String used to defined how to store the subscription of the user.
     */
    public abstract void subscribe(String username, Map<String, String> ctx);
}
