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

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.nuxeo.ecm.platform.notification.NotificationService;
import org.nuxeo.ecm.platform.notification.Subscriptions;
import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.runtime.api.Framework;

/**
 * Resolver class aims to be able to transform a CoreEvent to a Notification object
 *
 * @since XXX
 */
public abstract class Resolver {

    protected String id;

    public String getId() {
        return id == null ? this.getClass().getSimpleName() : id;
    }

    protected Resolver withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Check if the resolver is able to compute the current Event in a Notification Object.
     * <p>
     * The method is executed on EVERY core event, his resolution MUST be as fast as possible.
     *
     * @param eventRecord that could be transformed.
     * @return true if it assignable, false otherwise.
     */
    public abstract boolean accept(EventRecord eventRecord);

    /**
     * Resolve target users that have to be notified
     * <p>
     * The method is executed on EVERY corresponding notification, his resolution MUST be as fast as possible.
     *
     * @param eventRecord from the context that contain resolution needed
     * @return list of target users, of an empty list otherwise.
     */
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        Subscriptions subscribtions = Framework.getService(NotificationService.class)
                                                           .getSubscribtions(getId(),
                                                                   computeContextFromEvent(eventRecord));
        return subscribtions == null ? Stream.empty() : subscribtions.getUsernames();
    }

    /**
     * Compute Storage key context based on the given EventRecord
     * 
     * @param eventRecord contains informations needed to compute the key, in case we want to store subscriptions
     *            dependent of a context.
     * @return Context map, or an emptyMap in case the method hasn't been override.
     */
    protected Map<String, String> computeContextFromEvent(EventRecord eventRecord) {
        return Collections.emptyMap();
    }

    /**
     * Compute storage key depending of a context. For instance, to make a difference between subscribers of different
     * events, or a docId
     *
     * @param ctx A map of String used to defined what store to use.
     */
    public String computeSubscriptionsKey(Map<String, String> ctx) {
        return getId();
    }
}
