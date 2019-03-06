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
package org.nuxeo.ecm.notification;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.time.Duration;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.kv.KeyValueStoreProvider;
import org.nuxeo.runtime.stream.StreamHelper;

/**
 * @since 0.1
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

    public static Record readRecord(String group, String streamName) throws InterruptedException {
        NotificationStreamConfig streamConfig = Framework.getService(NotificationStreamConfig.class);
        // Check the record in the stream
        Codec<Record> codec = Framework.getService(CodecService.class).getCodec(DEFAULT_CODEC, Record.class);
        LogManager logManager = streamConfig.getLogManager(streamConfig.getLogConfigNotification());
        try (LogTailer<Record> tailer = logManager.createTailer(group, streamName, codec)) {
            LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(5));
            tailer.commit();
            return logRecord.message();
        }
        // never close the manager this is done by the service
    }

    public static void withUser(String username, String repositoryName, ThrowingConsumer<CoreSession> sessionYield) {
        try {
            LoginContext loginCtx = Framework.loginAsUser(username);
            NuxeoPrincipal principal = Framework.getService(UserManager.class).getPrincipal(username);
            try (CloseableCoreSession userSession = CoreInstance.openCoreSession(repositoryName, principal)) {
                sessionYield.accept(userSession);
            } finally {
                loginCtx.logout();
            }
        } catch (LoginException e) {
            throw new NuxeoException(e);
        }
    }
}
