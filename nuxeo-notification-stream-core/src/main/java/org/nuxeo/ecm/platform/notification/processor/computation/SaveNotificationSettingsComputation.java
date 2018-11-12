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
package org.nuxeo.ecm.platform.notification.processor.computation;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import org.nuxeo.ecm.platform.notification.NotificationStreamCallback;
import org.nuxeo.ecm.platform.notification.message.UserSettings;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;

/**
 * Computation saving the notification settings for a given user.
 *
 * @since XXX
 */
public class SaveNotificationSettingsComputation extends AbstractComputation {

    public final static String NAME = "saveNotificationSettings";

    public SaveNotificationSettingsComputation() {
        super(NAME, 1, 0);
    }

    @Override
    public void processRecord(ComputationContext computationContext, String s, Record record) {
        // Decode the settings in the record
        Codec<UserSettings> codec = Framework.getService(CodecService.class)
                                             .getCodec(DEFAULT_CODEC, UserSettings.class);
        UserSettings recordMessage = codec.decode(record.getData());

        // Update the settings
        String username = recordMessage.getUsername();
        NotificationStreamCallback scb = Framework.getService(NotificationStreamCallback.class);
        recordMessage.getSettingsMap()
                     .entrySet() //
                     .forEach(map -> scb.doUpdateSettings(username, map.getKey(), map.getValue().getSettings()));
    }
}
