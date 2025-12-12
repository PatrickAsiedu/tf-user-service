package app.tradeflows.api.user_service.events.listeners;

import app.tradeflows.api.user_service.configurations.JsonBuilder;
import app.tradeflows.api.user_service.dtos.TransactionDto;
import app.tradeflows.api.user_service.dtos.UserBalanceUpdateDTO;
import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.enums.BalanceAction;
import app.tradeflows.api.user_service.enums.TransactionStatus;
import app.tradeflows.api.user_service.enums.TransactionType;
import app.tradeflows.api.user_service.enums.UpdateType;
import app.tradeflows.api.user_service.repositories.AccountRepository;
import app.tradeflows.api.user_service.repositories.UserRepository;
import app.tradeflows.api.user_service.services.RedisService;
import app.tradeflows.api.user_service.services.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class KafkaConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final RedisService<Object> redisService;

    public KafkaConsumer(UserRepository userRepository, AccountRepository accountRepository,
                         TransactionService transactionService, RedisService<Object> redisService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.redisService = redisService;
    }

    @KafkaListener(topics = "${spring.kafka.topic.update-user-balance-topic}")
    public void consumeUserBalanceMessage(String message) {
        logger.info("Received message: " + message);
        UserBalanceUpdateDTO dto = new JsonBuilder().gson().fromJson(message, UserBalanceUpdateDTO.class);
        Optional<User> optionalUser = userRepository.findById(dto.getUserId());
        Optional<Account> optionalAccount = accountRepository.findByUser_Id(dto.getUserId());

        if(optionalUser.isEmpty() || optionalAccount.isEmpty()){
            logger.warn("The user or account not found");
            return;
        }

        User user = optionalUser.get();
        Account account = optionalAccount.get();

        if(dto.getType() == UpdateType.AVAILABLE_BALANCE){
            handleAvailableBalanceUpdate(dto, user, account);
        } else {
            handleLockAmountUpdate(dto, user, account);
        }

        // Save account and log transaction only once per message
        account.setUpdatedAt(LocalDateTime.now());
        var updatedAccount = accountRepository.save(account);
        redisService.addItem(user.getId(), updatedAccount);

        TransactionDto transactionDto = new TransactionDto(
                user,
                updatedAccount,
                TransactionType.valueOf(dto.getAction().name()),
                dto.getAmount(),
                TransactionStatus.SUCCESS,
                dto.getDescription()
        );
        transactionService.logUserTransaction(transactionDto, null);
    }

    private void handleLockAmountUpdate(UserBalanceUpdateDTO dto, User user, Account account){
        double newBalance;
        if(dto.getAction() == BalanceAction.CREDIT){
            newBalance = account.getLockedAmount() + dto.getAmount();
        }else{
            newBalance = account.getLockedAmount() - dto.getAmount();
        }
        account.setLockedAmount(newBalance);
    }

    private void handleAvailableBalanceUpdate(UserBalanceUpdateDTO dto, User user, Account account){
        double newBalance;
        if(dto.getAction() == BalanceAction.CREDIT){
            newBalance = account.getAvailableBalance() + dto.getAmount();
        }else{
            newBalance = account.getAvailableBalance() - dto.getAmount();
        }
        account.setAvailableBalance(newBalance);
    }
}