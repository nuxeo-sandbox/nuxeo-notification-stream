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
import org.nuxeo.ecm.platform.notification.dispatcher.Dispatcher;
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

    @Inject
    NotificationStreamCallback scb;

    @Test
    public void testDefaultNotificationSettings() {
        NotificationComponent cmp = (NotificationComponent) notif;
        assertThat(cmp.getDefaults("fileCreated").getSelectedDispatchers()).containsExactly("inApp");
        assertThat(cmp.getDefaults("fileUpdated").getSelectedDispatchers()).isEmpty();

        // Not *configured* Resolver must return a default settings
        assertThat(cmp.getDefaults("complexKey").getSettings().keySet()).containsExactlyInAnyOrder("inApp", "log");

        // Not *registered* Resolver must return null
        assertThat(cmp.getDefaults("unknown")).isNull();
    }

    @Test
    public void serviceReturnsDefaultSettingsIfUserHasNoneDefined() {
        NotificationComponent cmp = (NotificationComponent) notif;
        List<String> dispatchers = notif.getSelectedDispatchers("toto", "fileCreated") //
                                        .stream()
                                        .map(Dispatcher::getName)
                                        .collect(Collectors.toList());

        assertThat(dispatchers).isNotEmpty() //
                               .containsExactlyElementsOf(cmp.getDefaults("fileCreated").getSelectedDispatchers());
    }

    @Test
    public void serviceSavesUserPreferences() {
        Map<String, Boolean> settingsDispatcher = new HashMap<>();
        settingsDispatcher.put("inApp", false);
        settingsDispatcher.put("log", true);
        scb.doUpdateSettings("user1", "fileCreated", settingsDispatcher);

        // Fetch the settings for the user
        List<String> dispatchers = notif.getSelectedDispatchers("user1", "fileCreated") //
                                        .stream()
                                        .map(Dispatcher::getName)
                                        .collect(Collectors.toList());
        assertThat(dispatchers).containsExactlyInAnyOrder("log");
    }

    @Test
    public void serviceAddsDefaultDispatcher() {
        Map<String, Boolean> settingsDispatcher = new HashMap<>();
        settingsDispatcher.put("log", true);
        scb.doUpdateSettings("user1", "fileCreated", settingsDispatcher);

        // Fetch the settings for the user
        List<String> dispatchers = notif.getSelectedDispatchers("user1", "fileCreated") //
                                        .stream()
                                        .map(Dispatcher::getName)
                                        .collect(Collectors.toList());
        assertThat(dispatchers).containsExactlyInAnyOrder("inApp", "log");
    }

}
