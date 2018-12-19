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

package org.nuxeo.ecm.notification.computation;

import static org.nuxeo.runtime.stream.StreamServiceImpl.DEFAULT_CODEC;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.util.Locale;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.NotificationStreamConfig;
import org.nuxeo.ecm.notification.message.EventRecord;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.locale.LocaleProvider;
import org.nuxeo.ecm.user.center.profile.UserProfileService;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Computation that process an EventRecord to a Notification record.
 *
 * @since XXX
 */
public class EventToNotificationComputation extends AbstractComputation {

    public static final String NAME = "eventToNotificationComputation";

    public EventToNotificationComputation() {
        super(EventToNotificationComputation.NAME, 1, 1);
    }

    @Override
    public void processRecord(ComputationContext ctx, String inputStreamName, Record record) {
        NotificationStreamConfig notificationStreamConfig = Framework.getService(NotificationStreamConfig.class);
        String outputStream = notificationStreamConfig.getNotificationOutputStream();

        // Extract the EventRecord from the input Record
        EventRecord eventRecord = Framework.getService(CodecService.class) //
                                           .getCodec(DEFAULT_CODEC, EventRecord.class)
                                           .decode(record.getData());
        Framework.getService(NotificationService.class)
                 .getResolvers(eventRecord)
                 .forEach(r -> r.resolveTargetUsers(eventRecord)
                                .filter(user -> !user.equals(eventRecord.getUsername()))
                                .map(u -> {
                                    // Fetch the user locale
                                    Locale userLocale = getUserLocale(u, notificationStreamConfig.getRepositoryForUserLocale());
                                    return Notification.builder()
                                                       .fromEvent(eventRecord)
                                                       .withResolver(r, userLocale)
                                                       .withUsername(u)
                                                       .withCtx(r.buildNotifierContext(u, eventRecord))
                                                       .computeMessage()
                                                       .prepareEntities()
                                                       .resolveEntities()
                                                       .build();
                                })
                                .map(this::encodeNotif)
                                .forEach(notif -> ctx.produceRecord(outputStream, notif)));
        ctx.askForCheckpoint();
    }

    protected Record encodeNotif(Notification notif) {
        return Record.of(notif.getId(), Framework.getService(CodecService.class) //
                .getCodec(DEFAULT_CODEC, Notification.class)
                .encode(notif));
    }

    protected Locale getUserLocale(String username, String repositoryName) {
        Locale[] locale = new Locale[1];
        TransactionHelper.runInTransaction(() -> {
            // Fetch the user pref as username
            LoginContext loginContext;
            try {
                loginContext = Framework.loginAsUser(username);
                NuxeoPrincipal principal = Framework.getService(UserManager.class).getPrincipal(username);
                try (CloseableCoreSession userSession = CoreInstance.openCoreSession(repositoryName, principal)) {
                    UserProfileService userProfileService = Framework.getService(UserProfileService.class);
                    DocumentModel profile = userProfileService.getUserProfileDocument(username, userSession);
                    Locale localeUser = Framework.getService(LocaleProvider.class).getLocale(profile);
                    if (localeUser == null) {
                        localeUser = Locale.getDefault();
                    }
                    locale[0] = localeUser;
                } finally {
                    loginContext.logout();
                }
            } catch (LoginException e) {
                throw new NuxeoException(e);
            }
        });
        return locale[0];
    }
}
