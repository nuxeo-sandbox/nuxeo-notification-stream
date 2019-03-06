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

package org.nuxeo.ecm.notification.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.core.schema.types.constraints.Constraint.MESSAGES_BUNDLE;

import java.io.IOException;

import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.notification.notifier.Notifier;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since 0.1
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class NotifierJsonWriter extends ExtensibleEntityJsonWriter<Notifier> {

    public static final String ENTITY_TYPE = "notification-notifier";

    public NotifierJsonWriter() {
        super(ENTITY_TYPE, Notifier.class);
    }

    @Override
    protected void writeEntityBody(Notifier notifier, JsonGenerator jg) throws IOException {
        jg.writeStringField("name", notifier.getName());
        jg.writeStringField("label",
                I18NUtils.getMessageString(MESSAGES_BUNDLE, notifier.getLabelKey(), null, ctx.getLocale()));
        jg.writeStringField("description",
                I18NUtils.getMessageString(MESSAGES_BUNDLE, notifier.getDescriptionKey(), null, ctx.getLocale()));
    }
}
