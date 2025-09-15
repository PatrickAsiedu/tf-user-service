package app.tradeflows.api.user_service;

import app.tradeflows.api.user_service.configurations.KafkaProperties;
import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.exceptions.ConflictException;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.repositories.AccountRepository;
import app.tradeflows.api.user_service.services.AccountService;
import app.tradeflows.api.user_service.services.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AccountServiceTests {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private KafkaProperties properties;


    @InjectMocks
    private AccountService accountService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetUserAccountByEmail() throws NotFoundException {
        // Mock data
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        Account account = new Account(user, 100, 0, true);
        when(accountRepository.findByUser_Email(email)).thenReturn(Optional.of(account));

        // Test service method
        Account result = accountService.getUserAccountByEmail(email);

        // Verify
        assertEquals(account, result);
    }

    @Test
    void testGetUserAccountByEmail_NotFound() {
        // Mock data
        String email = "nonexistent@example.com";
        when(accountRepository.findByUser_Email(email)).thenReturn(Optional.empty());

        // Test and verify NotFoundException
        assertThrows(NotFoundException.class, () -> accountService.getUserAccountByEmail(email));
    }

    @Test
    void testGetUserAccountByUserId() throws NotFoundException {
        // Mock data
        String userId = "12345";
        User user = new User();
        user.setId("test");
        Account account = new Account(user, 100, 0, true);
        when(accountRepository.findByUser_Id(userId)).thenReturn(Optional.of(account));

        // Test service method
        Account result = accountService.getUserAccountByUserId(userId);

        // Verify
        assertEquals(account, result);
    }

    @Test
    void testGetUserAccountByUserId_NotFound() {
        // Mock data
        String userId = "nonexistent";
        when(accountRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        // Test and verify NotFoundException
        assertThrows(NotFoundException.class, () -> accountService.getUserAccountByUserId(userId));
    }

    @Test
    void testCreateAccountByUserEmail() {
        // Mock data
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        User user = new User();
        user.setEmail("newuser@example.com");
        when(accountRepository.findByUser_Email(user.getUsername())).thenReturn(Optional.empty());

        // Test service method
        assertDoesNotThrow(() -> accountService.createAccountByUserEmail(user, httpServletRequest));

        // Verify repository interactions
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    @Test
    void testCreateAccountByUserEmail_Conflict() {
        // Mock data
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        when(accountRepository.findByUser_Email(existingUser.getUsername())).thenReturn(Optional.of(new Account()));

        // Test and verify ConflictException
        assertThrows(ConflictException.class, () -> accountService.createAccountByUserEmail(existingUser, httpServletRequest));
    }
}
