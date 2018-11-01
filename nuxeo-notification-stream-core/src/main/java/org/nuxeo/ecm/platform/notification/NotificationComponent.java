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

package org.nuxeo.ecm.platform.notification;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.nuxeo.ecm.platform.notification.dispatcher.Dispatcher;
import org.nuxeo.ecm.platform.notification.dispatcher.DispatcherDescriptor;
import org.nuxeo.ecm.platform.notification.resolver.Resolver;
import org.nuxeo.ecm.platform.notification.resolver.ResolverDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

public class NotificationComponent extends DefaultComponent implements NotificationService {
    public static final String XP_DISPATCHER = "dispatcher";

    public static final String XP_RESOLVER = "resolver";

    protected Map<String, Dispatcher> dispatchers = new ConcurrentHashMap<>();

    protected Map<String, Resolver> resolvers = new ConcurrentHashMap<>();

    @Override
    public void unregisterContribution(Object contribution, String xp, ComponentInstance component) {
        super.unregisterContribution(contribution, xp, component);
        cleanupRegistry(contribution, xp);
    }

    @Override
    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        super.registerContribution(contribution, xp, component);

        // Ensure to reset current state when registering a new contribution
        cleanupRegistry(contribution, xp);
    }

    protected void cleanupRegistry(Object contribution, String xp) {
        if (!(contribution instanceof Descriptor)) {
            return;
        }

        Descriptor desc = (Descriptor) contribution;
        if (XP_DISPATCHER.equals(xp)) {
            dispatchers.remove(desc.getId());
        } else if (XP_RESOLVER.equals(xp)) {
            resolvers.remove(desc.getId());
        }
    }

    @Override
    public Dispatcher getDispatcher(String id) {
        return dispatchers.computeIfAbsent(id, s -> {
            Descriptor descriptor = getDescriptor(XP_DISPATCHER, s);
            if (descriptor != null) {
                return ((DispatcherDescriptor) descriptor).newInstance();
            }
            return null;
        });
    }

    @Override
    public Resolver getResolver(String id) {
        return resolvers.computeIfAbsent(id, s -> {
            Descriptor descriptor = getDescriptor(XP_RESOLVER, s);
            if (descriptor != null) {
                return ((ResolverDescriptor) descriptor).newInstance();
            }
            return null;
        });
    }
}
