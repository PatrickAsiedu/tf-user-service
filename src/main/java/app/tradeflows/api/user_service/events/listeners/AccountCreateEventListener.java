package app.tradeflows.api.user_service.events.listeners;

import app.tradeflows.api.user_service.configurations.JsonBuilder;
import app.tradeflows.api.user_service.configurations.KafkaProperties;
import app.tradeflows.api.user_service.dtos.AuditLogDto;
import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.events.AccountCreationEvent;
import app.tradeflows.api.user_service.events.AuditLogEvent;
import app.tradeflows.api.user_service.exceptions.ConflictException;
import app.tradeflows.api.user_service.services.AccountService;
import app.tradeflows.api.user_service.services.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class AccountCreateEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AccountCreateEventListener.class);
    private final AccountService accountService;
    private final RedisService<Object> redisService;

    public AccountCreateEventListener(AccountService accountService, RedisService<Object> redisService) {
        this.accountService = accountService;
        this.redisService = redisService;
    }

    @EventListener
    public void handleAccountCreationEvent(AccountCreationEvent event) throws ConflictException {

        try {
            Account account = accountService.getUserAccountByUserId(event.getUser().getId());
            redisService.addItem(event.getUser().getId(), new JsonBuilder().gson().toJson(account));
        }catch (Exception exception){
            logger.info(exception.toString(), exception);
            var account = accountService.createAccountByUserEmail(event.getUser(), event.getRequest());
            redisService.addItem(event.getUser().getId(), new JsonBuilder().gson().toJson(account));
        }
    }
}
