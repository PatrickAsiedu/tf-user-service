package app.tradeflows.api.user_service;

import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.events.publishers.EmailEventPublisher;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.repositories.UserRepository;
import app.tradeflows.api.user_service.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailEventPublisher emailEventPublisher;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAllUsers() {
        // Mock data
        List<User> users = Arrays.asList(
                new User("1", "Alice"),
                new User("2", "Bob"),
                new User("3", "Charlie")
        );
        when(userRepository.findAll()).thenReturn(users);

        // Test service method
        List<User> result = userService.allUsers();

        // Verify
        assertEquals(users.size(), result.size());
        assertEquals(users, result);
    }

    @Test
    void testGetUserById() throws NotFoundException {
        // Mock data
        String userId = "1";
        User user = new User(userId, "Alice");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Test service method
        User result = userService.getUserById(userId);

        // Verify
        assertEquals(user, result);
    }

    @Test
    void testGetUserById_NotFound() {
        // Mock data
        String userId = "nonexistent";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Test and verify NotFoundException
        assertThrows(NotFoundException.class, () -> userService.getUserById(userId));
    }
}
