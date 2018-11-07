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

package org.nuxeo.ecm.platform.notification.resolver.impl;

import org.nuxeo.ecm.platform.notification.message.EventRecord;
import org.nuxeo.ecm.platform.notification.resolver.Resolver;

/**
 * Resolver aims to ease DocumentEvent resolution
 * 
 * @since XXX
 */
public abstract class DocumentEventResolver extends Resolver {

    @Override
    public boolean accept(EventRecord eventRecord) {
        if (eventRecord.getDocumentSourceId() == null) {
            return false;
        }

        return acceptEventRecord(eventRecord);
    }

    public abstract boolean acceptEventRecord(EventRecord eventRecord);
}
