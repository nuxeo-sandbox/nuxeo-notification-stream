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

package org.nuxeo.ecm.restapi.server.jaxrs.notification.marschaller;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.notification.model.UserNotifierSettings;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @since XXX
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class UserNotifierSettingsJsonWriter extends ExtensibleEntityJsonWriter<UserNotifierSettings> {
    public static final String ENTITY_TYPE = "notification-user-settings-notifier";

    public UserNotifierSettingsJsonWriter() {
        super(ENTITY_TYPE, UserNotifierSettings.class);
    }

    @Override
    protected void writeEntityBody(UserNotifierSettings settings, JsonGenerator jg) throws IOException {
        for (Map.Entry<String, Boolean> notifierSettings : settings.getSettings().entrySet()) {
            jg.writeBooleanField(notifierSettings.getKey(), notifierSettings.getValue());
        }
    }
}
