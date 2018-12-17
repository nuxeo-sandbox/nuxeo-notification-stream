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
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.notification.message.EventRecord;

public class ContributorsResolver extends Resolver {

    @Override
    public boolean accept(EventRecord eventRecord) {
        return eventRecord.getEventName().equals(DocumentEventTypes.DOCUMENT_UPDATED);
    }

    @Override
    public Stream<String> resolveTargetUsers(EventRecord eventRecord) {
        return withDocument(eventRecord, (d) -> Arrays.stream((String[]) d.getPropertyValue("dc:contributors")));
    }

    @Override
    public Map<String, String> buildNotifierContext(String targetUsername, EventRecord eventRecord) {
        return Collections.emptyMap();
    }
}
