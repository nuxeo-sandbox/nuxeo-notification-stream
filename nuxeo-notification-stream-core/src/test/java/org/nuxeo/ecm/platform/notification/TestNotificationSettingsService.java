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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.notification.notifier.Notifier;
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
@Deploy("org.nuxeo.ecm.platform.notification.stream.core:OSGI-INF/basic-contrib.xml")
public class TestNotificationSettingsService {

    @Inject
    protected NotificationSettingsService nss;

    @Inject
    protected NotificationStreamCallback scb;

    @Test
    public void testDefaultNotificationSettings() {
        NotificationComponent cmp = (NotificationComponent) nss;
        assertThat(cmp.getDefaults("fileCreated").getSelectedNotifiers()).containsExactly("inApp");
        assertThat(cmp.getDefaults("fileUpdated").getSelectedNotifiers()).isEmpty();

        // Not *configured* Resolver must return a default settings
        assertThat(cmp.getDefaults("complexKey").getSettings().keySet()).containsExactlyInAnyOrder("inApp", "log");

        // Not *registered* Resolver must return null
        assertThat(cmp.getDefaults("unknown")).isNull();
    }

    @Test
    public void serviceReturnsDefaultSettingsIfUserHasNoneDefined() {
        NotificationComponent cmp = (NotificationComponent) nss;
        List<String> notifiers = nss.getSelectedNotifiers("toto", "fileCreated") //
                                      .stream()
                                      .map(Notifier::getName)
                                      .collect(Collectors.toList());

        assertThat(notifiers).isNotEmpty() //
                               .containsExactlyElementsOf(cmp.getDefaults("fileCreated").getSelectedNotifiers());
    }

    @Test
    public void serviceSavesUserPreferences() {
        Map<String, Boolean> settingsNotifier = new HashMap<>();
        settingsNotifier.put("inApp", false);
        settingsNotifier.put("log", true);
        scb.doUpdateSettings("user1", "fileCreated", settingsNotifier);

        // Fetch the settings for the user
        List<String> notifiers = nss.getSelectedNotifiers("user1", "fileCreated") //
                                      .stream()
                                      .map(Notifier::getName)
                                      .collect(Collectors.toList());
        assertThat(notifiers).containsExactlyInAnyOrder("log");
    }

    @Test
    public void serviceAddsDefaultNotifier() {
        Map<String, Boolean> settingsNotifier = new HashMap<>();
        settingsNotifier.put("log", true);
        scb.doUpdateSettings("user1", "fileCreated", settingsNotifier);

        // Fetch the settings for the user
        List<String> notifiers = nss.getSelectedNotifiers("user1", "fileCreated") //
                                      .stream()
                                      .map(Notifier::getName)
                                      .collect(Collectors.toList());
        assertThat(notifiers).containsExactlyInAnyOrder("inApp", "log");
    }
}
