/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification;

import java.util.concurrent.TimeUnit;

import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;

/**
 * @since XXX
 */
public class TestNotificationHelper {

    /**
     * Await for the lag of all stream to be 0.
     *
     * @param duration
     * @param unit
     * @return
     * @throws InterruptedException
     */
    public static boolean awaitCompletion(LogManager logManager, long duration, TimeUnit unit) throws InterruptedException {
        if (logManager == null) {
            return false;
        }

        long durationMs = unit.toMillis(duration);
        long deadline = System.currentTimeMillis() + durationMs;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            long lagTotal = logManager.listAll().stream().mapToLong(l ->
                    logManager.listConsumerGroups(l).stream().map(g -> logManager.getLag(l, g)).mapToLong(LogLag::lag).sum()
            ).sum();

            if (lagTotal == 0L) {
                return true;
            }
        }

        return false;
    }
}
