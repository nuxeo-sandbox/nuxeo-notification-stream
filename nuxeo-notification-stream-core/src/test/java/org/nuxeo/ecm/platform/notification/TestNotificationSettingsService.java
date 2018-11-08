/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Gildas Lefevre
 */
package org.nuxeo.ecm.platform.notification;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Test class for the service {@link NotificationSettingsService}.
 *
 * @since XXX
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.notification.stream.core")
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/dummy-contrib.xml")
public class TestNotificationSettingsService {

    @Inject
    NotificationSettingsService notif;

    @Test
    public void testDefaultNotificationSettings() {
        NotificationComponent cmp = (NotificationComponent) notif;
        assertThat(cmp.getDefaults("fileCreated")).containsExactly("inApp");
        assertThat(cmp.getDefaults("fileUpdated")).isEmpty();
        assertThat(cmp.getDefaults("unknown")).isNull();
    }

    @Test
    public void serviceReturnsDefaultSettingsIfUserHasNoneDefined() {

    }
}
