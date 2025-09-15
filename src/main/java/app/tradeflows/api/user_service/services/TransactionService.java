package app.tradeflows.api.user_service.services;

import app.tradeflows.api.user_service.dtos.AccountTopUpDto;
import app.tradeflows.api.user_service.dtos.TransactionDto;
import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.Transaction;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.events.publishers.AuditLogEventPublisher;
import app.tradeflows.api.user_service.events.publishers.EmailEventPublisher;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.repositories.AccountRepository;
import app.tradeflows.api.user_service.repositories.TransactionRepository;
import app.tradeflows.api.user_service.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final EmailEventPublisher emailEventPublisher;
    private final AuditLogEventPublisher auditLogEventPublisher;


    public void logUserTransaction(TransactionDto dto, HttpServletRequest request){
        Transaction transaction = new Transaction(dto.getAccount(), dto.getUser(), dto.getAmount(), dto.getDescription(), dto.getTransactionType(), dto.getTransactionStatus());
        transactionRepository.save(transaction);
        Context context = new Context();
        context.setVariable("name", dto.getUser().getName());
        context.setVariable("type", dto.getTransactionType());
        context.setVariable("amount", dto.getAmount());
        context.setVariable("balance", dto.getAccount().getAvailableBalance());
        context.setVariable("lockedBalance", dto.getAccount().getLockedAmount());
        context.setVariable("description", dto.getDescription());
        context.setVariable("date", LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
//        emailEventPublisher.publishEmailEvent(dto.getUser().getEmail(), "Your Account Transaction Notification", "transactionSuccess", context);
//        auditLogEventPublisher.publishLogEvent(dto.getUser().getId(), "TRANSACTION", dto.getUser().getName() + " | "+ dto.getDescription(), String.valueOf(dto.getUser().getRole()), request);

    }
}
