/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification.processors;

import java.util.Collections;
import java.util.Map;

import org.nuxeo.ecm.platform.notification.NotificationStreamConfig;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.stream.StreamProcessorTopology;

/**
 * Stream processor to update the user settings on notifications.
 *
 * @since XXX
 */
public class NotificationSettingsProcessor implements StreamProcessorTopology {

    @Override
    public Topology getTopology(Map<String, String> map) {
        return Topology.builder()
                       .addComputation(SaveNotificationSettingsComputation::new,
                               Collections.singletonList("i1:" + Framework.getService(NotificationStreamConfig.class)
                                                                          .getNotificationSettingsInputStream()))
                       .build();
    }
}
