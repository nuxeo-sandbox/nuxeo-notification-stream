/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.processors;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import org.nuxeo.ecm.platform.notification.NotificationSettingsService;
import org.nuxeo.ecm.platform.notification.message.UserSettingsRecord;
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
        Codec<UserSettingsRecord> codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC,
                UserSettingsRecord.class);
        UserSettingsRecord recordMessage = codec.decode(record.getData());

        // Update the settings
        String username = recordMessage.getUsername();
        NotificationSettingsService settingsService = Framework.getService(NotificationSettingsService.class);
        recordMessage.getSettingsMap()
                     .entrySet() //
                     .forEach(map -> settingsService.updateSettings(username, map.getKey(),
                             map.getValue().getSettings()));
    }
}
