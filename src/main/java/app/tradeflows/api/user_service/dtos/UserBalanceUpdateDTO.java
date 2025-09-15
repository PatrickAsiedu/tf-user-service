package app.tradeflows.api.user_service.dtos;

import app.tradeflows.api.user_service.enums.BalanceAction;
import app.tradeflows.api.user_service.enums.UpdateType;
import lombok.Data;

@Data
public class UserBalanceUpdateDTO {
    private String userId;
    private BalanceAction action;
    private double amount;
    private UpdateType type;
    private String description;
}
