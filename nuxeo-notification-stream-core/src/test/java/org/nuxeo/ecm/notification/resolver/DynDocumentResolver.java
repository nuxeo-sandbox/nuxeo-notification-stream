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

package org.nuxeo.ecm.notification.resolver;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.resolver.impl.SimpleSubscribableResolver;

public class DynDocumentResolver extends SubscribableResolver {
    public static final String DOC_ID_KEY = "docId";

    public static final String EVENT_KEY = "event";

    @Override
    public boolean accept(EventRecord eventRecord) {
        return StringUtils.isNotEmpty(eventRecord.getDocumentSourceId());
    }

    @Override
    public String computeSubscriptionsKey(Map<String, String> ctx) {
        return String.format("%s:%s:%s", getId(), ctx.get(DOC_ID_KEY), ctx.get(EVENT_KEY));
    }

    @Override
    public List<String> getRequiredContextFields() {
        return Arrays.asList(DOC_ID_KEY, EVENT_KEY);
    }

    @Override
    public Map<String, String> buildNotifierContext(EventRecord eventRecord) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put(DOC_ID_KEY, eventRecord.getDocumentSourceId());
        ctx.put(EVENT_KEY, eventRecord.getEventName());
        return ctx;
    }
}
