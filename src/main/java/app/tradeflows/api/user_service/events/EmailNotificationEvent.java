package app.tradeflows.api.user_service.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.thymeleaf.context.Context;

@Getter
public class EmailNotificationEvent extends ApplicationEvent {
    private final String recipient;
    private final String subject;
    private final String template;
    private final Context context;

    public EmailNotificationEvent(Object source, String recipient, String subject, String template, Context context) {
        super(source);
        this.recipient = recipient;
        this.subject = subject;
        this.template = template;
        this.context = context;
    }


}