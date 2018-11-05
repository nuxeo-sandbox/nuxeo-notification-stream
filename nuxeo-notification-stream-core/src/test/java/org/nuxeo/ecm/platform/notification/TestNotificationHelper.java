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

import org.mockito.Mockito;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;

/**
 * @since XXX
 */
public class TestNotificationHelper {

    /**
     * Await for the lag of all streams to be 0.
     *
     * @param logManager to use to resolve available logs
     * @param duration of the deadline before interrupting the wait, in unit
     * @param unit of the duration
     * @return true if lag is empty, false if the deadline is reached
     * @throws InterruptedException
     */
    public static boolean awaitCompletion(LogManager logManager, long duration, TimeUnit unit)
            throws InterruptedException {
        if (logManager == null) {
            return false;
        }

        long durationMs = unit.toMillis(duration);
        long deadline = System.currentTimeMillis() + durationMs;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            long lagTotal = logManager.listAll()
                                      .stream()
                                      .mapToLong(l -> logManager.listConsumerGroups(l)
                                                                .stream()
                                                                .map(g -> logManager.getLag(l, g))
                                                                .mapToLong(LogLag::lag)
                                                                .sum())
                                      .sum();

            if (lagTotal == 0L) {
                return true;
            }
        }

        return false;
    }

    public static Event buildEvent(CoreSession session, String docId, String docType, String event) {
        DocumentModel source = Mockito.mock(DocumentModel.class, Mockito.withSettings().serializable());
        Mockito.when(source.getType()).thenReturn(docType);
        Mockito.when(source.getId()).thenReturn(docId);

        return new DocumentEventContext(session, session.getPrincipal(), source).newEvent(event);
    }
}
