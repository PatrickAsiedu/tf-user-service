package app.tradeflows.api.user_service.events.listeners;

import app.tradeflows.api.user_service.configurations.EmailSender;
import app.tradeflows.api.user_service.events.EmailNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

@Component
public class EmailNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationListener.class);
    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;

    public EmailNotificationListener(EmailSender emailSender, TemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    @EventListener
    public void handleEmailNotificationEvent(EmailNotificationEvent event) {

        try {
            // Process the template with the given context
//            String htmlContent = templateEngine.process(event.getTemplate(), event.getContext());
//            emailSender.sendEmail(event.getRecipient(), event.getSubject(), htmlContent);
        }catch (Exception ex){
            logger.error("An error occurred sending email, {}", ex.getMessage());
        }
    }
}
