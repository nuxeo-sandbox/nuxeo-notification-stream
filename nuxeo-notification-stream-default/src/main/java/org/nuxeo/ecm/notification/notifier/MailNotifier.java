package org.nuxeo.ecm.notification.notifier;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.MessagingException;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.automation.context.ContextService;
import org.nuxeo.ecm.automation.core.mail.Composer;
import org.nuxeo.ecm.automation.core.mail.Mailer;
import org.nuxeo.ecm.automation.core.scripting.DateWrapper;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.notification.NotificationService;
import org.nuxeo.ecm.notification.entities.TextEntity;
import org.nuxeo.ecm.notification.entities.TextEntityFormatter;
import org.nuxeo.ecm.notification.entities.formatter.DefaultTextEntityFormatter;
import org.nuxeo.ecm.notification.message.Notification;
import org.nuxeo.ecm.notification.resolver.Resolver;
import org.nuxeo.ecm.notification.resolver.SubscribableResolver;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

import freemarker.template.TemplateException;

public class MailNotifier extends Notifier {
    private static final Logger log = LogManager.getLogger(MailNotifier.class);

    protected static final Composer COMPOSER = new Composer();

    public MailNotifier(NotifierDescriptor desc) {
        super(desc);
    }

    @Override
    public void process(Notification notification) {
        try {
            NuxeoPrincipal targetUser = Framework.getService(UserManager.class)
                                                 .getPrincipal(notification.getUsername());
            if (targetUser == null || StringUtils.isEmpty(targetUser.getEmail())) {
                log.warn("Unable to send notification to user: {} (notificationId: {})", notification.getUsername(),
                        notification.getId());
                return;
            }

            Mailer.Message message = COMPOSER.newHtmlMessage(getBody(notification), initRenderingCtx(notification));
            message.setSubject(formatMessageForSubject(notification), "UTF-8");
            message.setSentDate(new Date());

            message.addTo(targetUser.getEmail());
            message.addFrom(Framework.getProperty("mail.from"));

            message.send();
        } catch (MessagingException e) {
            log.error("Unable to connect to mail server", e);
            if (!Framework.isTestModeSet()) {
                throw new NuxeoException(e);
            }
        } catch (IOException | TemplateException e) {
            log.warn("Inner exception while trying to configure notification's email", e);
            throw new NuxeoException(e);
        }
    }

    protected URL getTemplateURL(Notification notification) {
        String template = getProperty("template-" + notification.getResolverId(), getProperty("template-default"));
        return this.getClass().getClassLoader().getResource(template);
    }

    protected String getBody(Notification notification) throws IOException {
        try (InputStream in = getTemplateURL(notification).openStream()) {
            return IOUtils.toString(in, UTF_8);
        }
    }

    protected Map<String, Object> initRenderingCtx(Notification notification) {
        Map<String, Object> map = new HashMap<>(notification.getContext());

        map.put("This", notification);
        map.put("CurrentDate", new DateWrapper());
        map.put("Env", Framework.getProperties());
        map.put("Runtime", Framework.getRuntime());
        map.put("baseUrl", Framework.getProperty("nuxeo.url"));
        map.put("unsubUrl", buildUnsubURL(notification));
        map.put("message", formatMessageForMail(notification));

        Map<String, Object> entities = new HashMap<>();
        notification.getEntities().forEach(e -> entities.put(e.getId(), e.getValues()));
        map.put("entities", entities);

        // Helpers injection
        ContextService contextService = Framework.getService(ContextService.class);
        map.putAll(contextService.getHelperFunctions());

        return map;
    }

    protected String buildUnsubURL(Notification notification) {
        UriBuilder builder = UriBuilder.fromUri(Framework.getProperty("nuxeo.url", "http://localhost:8080/nuxeo"))
                                       .path("api")
                                       .path("notification")
                                       .path("resolver");
        Resolver res = Framework.getService(NotificationService.class).getResolver(notification.getResolverId());
        if (!(res instanceof SubscribableResolver)) {
            return null;
        }
        ((SubscribableResolver) res).getRequiredContextFields()
                                    .forEach(s -> builder.queryParam(s, notification.getContext().get(s)));

        return builder.build().toString();
    }

    protected String formatMessageForSubject(Notification notification) {
        return TextEntityFormatter.format(DefaultTextEntityFormatter.class, notification);
    }

    protected String formatMessageForMail(Notification notification) {
        return TextEntityFormatter.format(MailTextEntityFormatter.class, notification);
    }

    public static final class MailTextEntityFormatter implements TextEntityFormatter {
        @Override
        public String format(String message, List<TextEntity> entities) {
            if (entities.size() == 0) {
                return message;
            }

            StringBuilder sb = new StringBuilder();
            AtomicInteger lastEntityEnd = new AtomicInteger(0);
            entities.forEach(e -> {
                sb.append(message, lastEntityEnd.get(), e.getStart());
                if (e.getType().equals(TextEntity.DOCUMENT)) {
                    sb.append("<a class=\"entity-doc\" href=\"")
                      .append(e.getValue("url"))
                      .append("\">")
                      .append(e.getTitle())
                      .append("</a>");
                } else {
                    sb.append("<span class=\"entity-")
                      .append(e.getType())
                      .append("\">")
                      .append(e.getTitle())
                      .append("<span>");
                }
                lastEntityEnd.set(e.getEnd());
            });
            sb.append(message.substring(lastEntityEnd.get()));

            return sb.toString();
        }
    }
}
