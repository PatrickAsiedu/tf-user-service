package app.tradeflows.api.user_service.dtos;

import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.enums.TransactionStatus;
import app.tradeflows.api.user_service.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    private User user;
    private Account account;
    private TransactionType transactionType;
    private double amount;
    private TransactionStatus transactionStatus;
    private String description;
}
