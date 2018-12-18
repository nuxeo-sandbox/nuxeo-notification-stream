package org.nuxeo.ecm.notification.notifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.nuxeo.ecm.notification.message.Notification.ORIGINATING_USER;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.entities.TextEntitiesReplacer;
import org.nuxeo.ecm.notification.entities.TextEntity;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features({ NotificationFeature.class, AutomationFeature.class })
public class TestMailNotifier {
    @Test
    public void testTextFormatter() {
        MailNotifier.MailTextEntityFormatter formatter = new MailNotifier.MailTextEntityFormatter();
        String message = "Hello @{user:toto} and doc @{doc:000-000}";
        String formatted = formatter.format(message, Collections.emptyList());
        assertThat(formatted).isEqualTo(message);

        List<TextEntity> entities = TextEntitiesReplacer.from(message).buildTextEntities();

        entities.get(0).getValues().put("title", "BFF Toto");
        entities.get(1).getValues().put("title", "Best Doc Ever");
        entities.get(1).getValues().put("url", "fake");

        formatted = formatter.format(message, entities);
        assertThat(formatted).isEqualTo(
                "Hello <span class=\"entity-user\">BFF Toto<span> and doc <a class=\"entity-doc\" href=\"fake\">Best Doc Ever</a>");
    }

    @Test
    public void testRenderingCtx() {
        Notification notif = Notification.builder()
                                         .withUsername("Administrator")
                                         .withMessage("Hello World @{user:Administrator}")
                                         .withCtx(ORIGINATING_USER, "Administrator")
                                         .prepareEntities()
                                         .resolveEntities()
                                         .build();

        NotifierDescriptor desc = mock(NotifierDescriptor.class);
        desc.id = "dummy-id";

        MailNotifier notifier = new MailNotifier(desc);

        Map<String, Object> ctx = notifier.initRenderingCtx(notif);
        Map<String, Object> entities = (Map<String, Object>) ctx.get("entities");
        assertThat(entities.keySet()).containsExactly("Administrator");
        assertThat((Map<String, String>) entities.get("Administrator")).containsKeys("username", "title", "firstName",
                "lastName", "email");
    }
}