package app.tradeflows.api.user_service.events.listeners;

import app.tradeflows.api.user_service.configurations.EmailSender;
import app.tradeflows.api.user_service.configurations.JsonBuilder;
import app.tradeflows.api.user_service.configurations.KafkaProperties;
import app.tradeflows.api.user_service.dtos.AuditLogDto;
import app.tradeflows.api.user_service.events.AuditLogEvent;
import app.tradeflows.api.user_service.events.EmailNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class AuditLogListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogListener.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties properties;

    public AuditLogListener(KafkaTemplate<String, String> kafkaTemplate, KafkaProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @EventListener
    public void handleAuditLogEvent(AuditLogEvent event) {

        try {
            // Process the template with the given context
            AuditLogDto dto = new AuditLogDto();
            dto.setAction(event.getAction());
            dto.setRole(event.getRole());
            dto.setDescription(event.getDescription());
            dto.setTimestamp(LocalDateTime.now());
            dto.setUserId(event.getUserId());
            dto.setSystemComponent("USER-SERVICE");

            if(Objects.nonNull(event.getRequest())){
                Map<String, String> metaData = new HashMap<>();
                dto.setIpAddress(event.getRequest().getHeader("X-CLIENT-IP"));
                metaData.put("X-CLIENT-IP", event.getRequest().getHeader("X-CLIENT-IP"));
                metaData.put("SESSION-ID", event.getRequest().getRequestedSessionId());
                dto.setMetaData(metaData);
            }
            String payload = new JsonBuilder().gson().toJson(dto);
            kafkaTemplate.send(properties.getAuditLogReportingTopic(), payload);
        }catch (Exception ex){
            logger.error("An error occurred publishing to topic, {}", ex.getMessage());
        }
    }
}
