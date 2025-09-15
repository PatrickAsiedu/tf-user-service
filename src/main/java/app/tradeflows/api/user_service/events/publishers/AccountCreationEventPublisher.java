package app.tradeflows.api.user_service.events.publishers;

import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.events.AccountCreationEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class AccountCreationEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public AccountCreationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishEvent(User user, HttpServletRequest request) {
        AccountCreationEvent event = new AccountCreationEvent(this, user, request);
        eventPublisher.publishEvent(event);
    }
}
