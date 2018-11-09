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

package org.nuxeo.ecm.platform.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(NotificationFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/dummy-contrib.xml")
public class TestNotificationSubscriptionsProcessor {

    @Inject
    protected NotificationService notif;

    @Inject
    protected NotificationStreamConfig config;

    protected String username = "tommy";

    protected String resolverId = "fileCreated";

    protected Map<String, String> ctx = Collections.emptyMap();

    @Test
    public void testSubscribeAndUnsubscribe() throws InterruptedException {
        assertThat(notif.getSubscriptions(resolverId, ctx).getUsernames()).isEmpty();

        notif.subscribe(username, resolverId, ctx);

        TestNotificationHelper.awaitCompletion(config.getLogManager(), 5, TimeUnit.SECONDS);
        assertThat(notif.getSubscriptions(resolverId, ctx).getUsernames()).containsExactly(username);

        notif.unsubscribe(username, resolverId, ctx);

        TestNotificationHelper.awaitCompletion(config.getLogManager(), 5, TimeUnit.SECONDS);
        assertThat(notif.getSubscriptions(resolverId, ctx).getUsernames()).isEmpty();
    }
}
