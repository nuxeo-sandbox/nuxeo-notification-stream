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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @since XXX
 */
@XObject("resolver")
public class ResolverDescriptor implements Descriptor {
    @XNode("@id")
    protected String id;

    @XNode("@class")
    protected Class<? extends Resolver> resolverClass;

    @XNode("@order")
    protected int order = 100;

    @Override
    public String getId() {
        return id;
    }

    public Resolver newInstance() {
        try {
            return resolverClass.newInstance().init(this);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    public int getOrder() {
        return order;
    }
}