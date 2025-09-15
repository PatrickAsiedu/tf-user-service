package app.tradeflows.api.user_service.dtos;

import app.tradeflows.api.user_service.enums.TransactionStatus;
import app.tradeflows.api.user_service.enums.TransactionType;
import lombok.Data;

@Data
public class AccountTopUpDto {
    private String userId;
    private TransactionType transactionType;
    private double amount;
}
