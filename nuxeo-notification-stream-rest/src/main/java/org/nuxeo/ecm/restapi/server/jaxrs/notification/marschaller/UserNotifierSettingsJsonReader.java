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

import static org.nuxeo.ecm.core.io.registry.MarshallingConstants.ENTITY_FIELD_NAME;
import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;

import org.nuxeo.ecm.core.io.marshallers.json.EntityJsonReader;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.notification.model.UserNotifierSettings;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since XXX
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class UserNotifierSettingsJsonReader extends EntityJsonReader<UserNotifierSettings> {

    public UserNotifierSettingsJsonReader() {
        super(UserNotifierSettingsJsonWriter.ENTITY_TYPE);
    }

    @Override
    protected UserNotifierSettings readEntity(JsonNode jsonNode) throws IOException {
        UserNotifierSettings.UserNotifierSettingsBuilder builder = UserNotifierSettings.builder();

        jsonNode.fields().forEachRemaining((field) -> {
            if (field.getKey().equals(ENTITY_FIELD_NAME)) {
                return;
            }

            builder.putSetting(field.getKey(), field.getValue().booleanValue());
        });

        return builder.build();
    }
}
