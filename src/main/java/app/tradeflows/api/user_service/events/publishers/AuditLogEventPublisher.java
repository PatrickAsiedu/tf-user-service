package app.tradeflows.api.user_service.events.publishers;

import app.tradeflows.api.user_service.events.AuditLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuditLogEventPublisher {
    private final ApplicationEventPublisher eventPublisher;

    public AuditLogEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishLogEvent(String userId, String action, String description, String role, HttpServletRequest request) {
        AuditLogEvent event = new AuditLogEvent(this, userId, action, description, role, request);
        eventPublisher.publishEvent(event);
    }
}
