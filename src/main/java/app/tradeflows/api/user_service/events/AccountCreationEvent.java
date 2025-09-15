package app.tradeflows.api.user_service.events;

import app.tradeflows.api.user_service.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.thymeleaf.context.Context;

@Getter
public class AccountCreationEvent extends ApplicationEvent {
    private final User user;
    private final  HttpServletRequest request;

    public AccountCreationEvent(Object source, User user, HttpServletRequest request) {
        super(source);
        this.user = user;
        this.request = request;
    }


}