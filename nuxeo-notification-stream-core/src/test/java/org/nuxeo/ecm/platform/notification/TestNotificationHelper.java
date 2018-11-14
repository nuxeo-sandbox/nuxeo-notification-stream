/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.stream.StreamHelper;

/**
 * @since XXX
 */
public class TestNotificationHelper {

    /**
     * Await for notification processor to drain and stop all his computations
     *
     * @return {@code true} if computations are stopped during the timeout delay.
     */
    public static boolean waitProcessorsCompletion() {
        return StreamHelper.drainAndStop();
    }

    public static void clearKVS(String name) {
        NotificationComponent nc = (NotificationComponent) Framework.getService(NotificationService.class);
        ((KeyValueStoreProvider) nc.getKeyValueStore(name)).clear();
    }
}
