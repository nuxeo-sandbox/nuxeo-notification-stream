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

import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.notification.resolver.impl.DocumentEventResolver;

public class FileCreatedResolver extends DocumentEventResolver {

    @Override
    public List<String> resolveTargetUsers(DocumentModel doc) {
        return null;
    }

    @Override
    public boolean accept(Event event, DocumentEventContext ctx, DocumentModel source) {
        return source.getType().equals("File") && event.getName().equals(DOCUMENT_CREATED);
    }

    @Override
    public void subscribe(String username, Map<String, String> ctx) {
        // Not required for the tests
    }
}
