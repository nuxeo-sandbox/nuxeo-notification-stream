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

import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_DOC_ID;
import static org.nuxeo.ecm.notification.message.EventRecord.SOURCE_EVENT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.notification.message.EventRecord;

public class DynDocumentResolver extends SubscribableResolver {

    @Override
    public boolean accept(EventRecord eventRecord) {
        return StringUtils.isNotEmpty(eventRecord.getDocumentSourceId());
    }

    @Override
    public String computeSubscriptionsKey(Map<String, String> ctx) {
        return String.format("%s:%s:%s", getId(), ctx.get(SOURCE_DOC_ID), ctx.get(SOURCE_EVENT));
    }

    @Override
    public List<String> getRequiredContextFields() {
        return Arrays.asList(SOURCE_DOC_ID, SOURCE_EVENT);
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        return Collections.emptyMap();
    }
}
