package app.tradeflows.api.user_service.dtos;

import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.User;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserDTO extends User {
    private Account account;
}
