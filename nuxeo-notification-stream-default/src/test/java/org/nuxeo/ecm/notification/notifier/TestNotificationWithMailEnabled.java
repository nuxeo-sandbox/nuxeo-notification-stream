package org.nuxeo.ecm.notification.notifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.nuxeo.ecm.notification.message.Notification.ORIGINATING_USER;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.FakeSmtpMailServerFeature;
import org.nuxeo.ecm.notification.NotificationFeature;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.dumbster.smtp.SmtpMessage;

@RunWith(FeaturesRunner.class)
@Features({ NotificationFeature.class, AutomationFeature.class, FakeSmtpMailServerFeature.class })
@Deploy("org.nuxeo.ecm.platform.notification.stream.default")
public class TestNotificationWithMailEnabled {
    @Inject
    protected CoreSession session;

    @Inject
    protected NotificationService ns;

    @Before
    public void before() {
        Framework.getProperties().put("mail.from", "devnull@nuxeo.com");
    }

    @Test
    public void testProcessMail() {
        DocumentModel doc = session.createDocumentModel("/", "foobar", "File");
        doc.setPropertyValue("dc:title", "my-wonderful-doc");
        doc = session.createDocument(doc);
        session.save();

        Notification notif = Notification.builder()
                                         .withUsername("Administrator")
                                         .withResolver(ns.getResolver("documentUpdated"))
                                         .withMessage(
                                                 "User @{user:Administrator} updated doc @{doc:" + doc.getId() + "}")
                                         .withCtx(ORIGINATING_USER, "Administrator")
                                         .withSourceId(doc.getId())
                                         .withSourceRepository(doc.getRepositoryName())
                                         .prepareEntities()
                                         .resolveEntities()
                                         .build();

        NotifierDescriptor desc = mock(NotifierDescriptor.class);
        desc.id = "dummy-id";

        Notifier mail = ns.getNotifier("mail");
        mail.process(notif);
    }
}
