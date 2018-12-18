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

import static org.nuxeo.ecm.notification.NotificationComponent.REPOSITORY_USER_PROFILE_PROP;

import org.junit.runners.model.FrameworkMethod;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.notification.notifier.CounterNotifier;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

/**
 * @since XXX
 */
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core")
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.types.core")
@Deploy("org.nuxeo.ecm.platform.url.core")
@Deploy("org.nuxeo.ecm.user.center.profile")
@Deploy("org.nuxeo.ecm.platform.userworkspace.api")
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
@Deploy("org.nuxeo.ecm.platform.web.common")
public class NotificationFeature implements RunnerFeature {
    @Override
    public void beforeMethodRun(FeaturesRunner runner, FrameworkMethod method, Object test) throws Exception {
        CounterNotifier.reset();
    }

    @Override
    public void start(FeaturesRunner runner) {
        // Configure the repository name to fetch the user locale
        Framework.getProperties().setProperty(REPOSITORY_USER_PROFILE_PROP, "test");
    }
}
