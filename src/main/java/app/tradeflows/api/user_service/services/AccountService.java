package app.tradeflows.api.user_service.services;

import app.tradeflows.api.user_service.configurations.JsonBuilder;
import app.tradeflows.api.user_service.configurations.KafkaProperties;
import app.tradeflows.api.user_service.dtos.AccountTopUpDto;
import app.tradeflows.api.user_service.dtos.TransactionDto;
import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.enums.TransactionStatus;
import app.tradeflows.api.user_service.enums.TransactionType;
import app.tradeflows.api.user_service.exceptions.ConflictException;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.repositories.AccountRepository;
import app.tradeflows.api.user_service.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaProperties properties;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, TransactionService transactionService,
                          KafkaTemplate<String, String> kafkaTemplate, KafkaProperties properties,
                          UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.userRepository = userRepository;
    }

    public Account getUserAccountByEmail(String email) throws NotFoundException {
        Optional<Account> account = accountRepository.findByUser_Email(email);
        return account.orElseThrow(() -> new NotFoundException("User account not found"));
    }

    public Account getUserAccountByUserId(String userId) throws NotFoundException {
        Optional<Account> account = accountRepository.findByUser_Id(userId);
        return account.orElseThrow(() -> new NotFoundException("User account not found"));
    }

    @Transactional
    public Account createAccountByUserEmail(User user, HttpServletRequest request) throws ConflictException {
        Optional<Account> account = accountRepository.findByUser_Email(user.getUsername());
        if (account.isPresent()) {
            throw new ConflictException("Account already exists");
        }

        double INITIAL_BALANCE = 100;
        double INITIAL_LOCKED_AMOUNT = 0;
        Account newAccount = new Account(user, INITIAL_BALANCE, INITIAL_LOCKED_AMOUNT, true);
        var createdAccount = accountRepository.save(newAccount);
        TransactionDto transactionDto = new TransactionDto(user, createdAccount, TransactionType.CREDIT,INITIAL_BALANCE, TransactionStatus.SUCCESS, "Reward - Sign up Bonus");
        transactionService.logUserTransaction(transactionDto, request);
        kafkaTemplate.send(properties.getCreateDefaultPortfolioTopic(), user.getId());
        return createdAccount;
    }

    @Transactional
    public void topUpAccount(AccountTopUpDto dto, HttpServletRequest request) throws NotFoundException {
        String description = "";
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new NotFoundException("User not found"));

        Optional<Account> optionalAccount = accountRepository.findByUser_Id(user.getId());
        Account account = optionalAccount.orElseGet(() -> new Account(user, 0, 0, true));
        if(Objects.equals(dto.getTransactionType(), TransactionType.CREDIT)){
            account.setAvailableBalance(account.getAvailableBalance() + dto.getAmount());
            description = "Account top-up of "+dto.getAmount();
        }else{
            account.setAvailableBalance(account.getAvailableBalance() - dto.getAmount());
            description = "Account withdrawal of "+dto.getAmount();
        }
        accountRepository.save(account);
        TransactionDto transactionDto = new TransactionDto(user, account, dto.getTransactionType(), dto.getAmount(), TransactionStatus.SUCCESS, description);
        transactionService.logUserTransaction(transactionDto, request);
    }
}
