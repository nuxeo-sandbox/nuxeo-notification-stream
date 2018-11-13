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

import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.ecm.platform.notification.resolver.impl.SimpleSubscribableResolver;

public class ComplexSubsKeyResolver extends SimpleSubscribableResolver {

    public static final String NAME_FIELD = "name";

    public static final String SUFFIX_FIELD = "name";

    @Override
    public boolean accept(EventRecord eventRecord) {
        return false;
    }

    @Override
    public String computeSubscriptionsKey(Map<String, String> ctx) {
        return String.format("%s:%s", ctx.getOrDefault(NAME_FIELD, "name"), ctx.getOrDefault(SUFFIX_FIELD, "suffix"));
    }

    @Override
    public Map<String, String> buildNotifierContext(EventRecord eventRecord) {
        return Collections.emptyMap();
    }
}
