package app.tradeflows.api.user_service.events.publishers;

import app.tradeflows.api.user_service.events.EmailNotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Component
public class EmailEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public EmailEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEmailEvent(String recipient, String subject, String template, Context context) {
        EmailNotificationEvent event = new EmailNotificationEvent(this, recipient, subject, template, context);
        eventPublisher.publishEvent(event);
    }
}
