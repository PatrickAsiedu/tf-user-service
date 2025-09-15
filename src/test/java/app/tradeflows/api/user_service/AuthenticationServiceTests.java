package app.tradeflows.api.user_service;

import app.tradeflows.api.user_service.configurations.JsonBuilder;
import app.tradeflows.api.user_service.dtos.ResetPasswordDto;
import app.tradeflows.api.user_service.dtos.LoginUserDto;
import app.tradeflows.api.user_service.dtos.RegisterUserDto;
import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.enums.UserRole;
import app.tradeflows.api.user_service.events.publishers.AccountCreationEventPublisher;
import app.tradeflows.api.user_service.events.publishers.AuditLogEventPublisher;
import app.tradeflows.api.user_service.events.publishers.EmailEventPublisher;
import app.tradeflows.api.user_service.exceptions.ConflictException;
import app.tradeflows.api.user_service.exceptions.InvalidCredentialExcetion;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.exceptions.PasswordMismatchException;
import app.tradeflows.api.user_service.repositories.UserRepository;
import app.tradeflows.api.user_service.responses.LoginResponse;
import app.tradeflows.api.user_service.services.AccountService;
import app.tradeflows.api.user_service.services.AuthenticationService;
import app.tradeflows.api.user_service.services.JwtService;
import app.tradeflows.api.user_service.services.RedisService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthenticationServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AccountService accountService;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailEventPublisher emailEventPublisher;

    @Mock
    private AuditLogEventPublisher auditLogEventPublisher;

    @Mock
    private RedisService<Object> redisService;

    @Mock
    private AccountCreationEventPublisher accountCreationEventPublisher;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUser_success() throws ConflictException {
        RegisterUserDto request = new RegisterUserDto("John Doe", "john.doe@example.com", "password123", "1990-01-01");
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        User newUser = new User("John Doe", "john.doe@example.com", "encodedPassword", UserRole.USER, "1990-01-01", false);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(jwtService.generateToken(newUser)).thenReturn("token");

        authenticationService.registerUser(request, httpServletRequest);

        verify(userRepository).findByEmail(request.getEmail());
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(newUser);
        verify(emailEventPublisher).publishEmailEvent(eq("john.doe@example.com"), anyString(), eq("emailConfirmation"), any(Context.class));
        verify(auditLogEventPublisher).publishLogEvent(eq(null), eq("REGISTER"), eq("John Doe Registered an account"), eq("USER"), eq(httpServletRequest));
    }

    @Test
    void testRegisterUser_emailConflict() throws ConflictException {
        RegisterUserDto request = new RegisterUserDto("John Doe", "john.doe@example.com", "password123", "1990-01-01");
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        User existingUser = new User("John Doe", "john.doe@example.com", "encodedPassword", UserRole.USER, "1990-01-01", false);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));

        ConflictException thrown = assertThrows(ConflictException.class, () -> {
            authenticationService.registerUser(request, httpServletRequest);
        });

        assertEquals("Email already exists", thrown.getMessage());
    }

    @Test
    void testAuthenticate_success() throws InvalidCredentialExcetion, NotFoundException, ConflictException {
        LoginUserDto input = new LoginUserDto("john.doe@example.com", "password123");
        HttpServletRequest request = mock(HttpServletRequest.class);
        User authenticatedUser = new User("John Doe", "john.doe@example.com", "encodedPassword", UserRole.USER, "1990-01-01", true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail(input.getEmail())).thenReturn(Optional.of(authenticatedUser));
        when(jwtService.generateToken(authenticatedUser)).thenReturn("jwtToken");
        when(jwtService.getExpirationTime()).thenReturn(3600L);
        when(accountService.getUserAccountByUserId(authenticatedUser.getId())).thenReturn(any(Account.class));

        LoginResponse response = authenticationService.authenticate(input, request);

        assertEquals("jwtToken", response.getToken());
        assertEquals(3600, response.getExpiresIn());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(input.getEmail());
        verify(jwtService).generateToken(authenticatedUser);
        verify(auditLogEventPublisher).publishLogEvent(eq(null), eq("LOGIN"), eq("John Doe logged to their account"), eq("USER"), eq(request));
    }

    @Test
    void testAuthenticate_invalidCredentials() {
        LoginUserDto input = new LoginUserDto("john.doe@example.com", "wrongPassword");
        HttpServletRequest request = mock(HttpServletRequest.class);

        InvalidCredentialExcetion thrown = assertThrows(InvalidCredentialExcetion.class, () -> {
            authenticationService.authenticate(input, request);
        });

        assertEquals("User/password invalid", thrown.getMessage());
    }

    @Test
    void testConfirmUser_success() throws NotFoundException, ConflictException {
        String token = "validToken";
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User("John Doe", "john.doe@example.com", "encodedPassword", UserRole.USER, "1990-01-01", false);

        when(jwtService.decodeToken(token)).thenReturn("john.doe@example.com");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        authenticationService.confirmUser(token, request);

        verify(userRepository).findByEmail("john.doe@example.com");
        verify(userRepository).save(any(User.class));
        verify(accountService).createAccountByUserEmail(user, request);
        verify(auditLogEventPublisher).publishLogEvent(eq(null), eq("CONFIRM EMAIL"), eq("John Doe confirm to their account"), eq("USER"), eq(request));
    }

    @Test
    void testConfirmUser_userNotFound() {
        String token = "invalidToken";
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(jwtService.decodeToken(token)).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            authenticationService.confirmUser(token, request);
        });

        assertEquals("User does not exists", thrown.getMessage());
    }

    @Test
    void testConfirmUser_alreadyActive() throws NotFoundException, ConflictException {
        String token = "validToken";
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User("John Doe", "john.doe@example.com", "encodedPassword", UserRole.USER, "1990-01-01", true);

        when(jwtService.decodeToken(token)).thenReturn("john.doe@example.com");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        authenticationService.confirmUser(token, request);

        verify(userRepository).findByEmail("john.doe@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(accountService, never()).createAccountByUserEmail(user, request);
        verify(auditLogEventPublisher, never()).publishLogEvent(eq(null), eq("CONFIRM EMAIL"), eq("John Doe confirm to their account"), eq("USER"), eq(request));
    }

    @Test
    void testForgotPassword_success() throws NotFoundException, MessagingException, UnsupportedEncodingException {
        String email = "john.doe@example.com";
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User("John Doe", "john.doe@example.com", "encodedPassword", UserRole.USER, "1990-01-01", true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user, 60 * 60 * 60)).thenReturn("resetToken");
        Context context = new Context();
        context.setVariable("action_url", "https://tradeflows.app/reset-password?token=resetToken");

        authenticationService.forgotPassword(email, request);

        verify(userRepository).findByEmail(email);
        verify(jwtService).generateToken(user, 60 * 60 * 60);
        verify(emailEventPublisher).publishEmailEvent(eq(email), anyString(), eq("passwordResetEmail"), any(Context.class));
        verify(auditLogEventPublisher).publishLogEvent(eq(null), eq("FORGOT PASSWORD"), eq("John Doe forgot their password"), eq("USER"), eq(request));
    }

    @Test
    void testForgotPassword_userNotFound() {
        String email = "nonexistent@example.com";
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            authenticationService.forgotPassword(email, request);
        });

        assertEquals("User not found", thrown.getMessage());
    }

    @Test
    void testResetPassword_success() throws NotFoundException, PasswordMismatchException {
        String token = "validToken";
        ResetPasswordDto dto = new ResetPasswordDto("newPassword123", "newPassword123");
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User("John Doe", "john.doe@example.com", "oldPassword", UserRole.USER, "1990-01-01", true);

        when(jwtService.decodeToken(token)).thenReturn("john.doe@example.com");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(dto.getNewPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        authenticationService.resetPassword(token, dto, request);

        verify(userRepository).findByEmail("john.doe@example.com");
        verify(passwordEncoder).encode(dto.getNewPassword());
        verify(userRepository).save(any(User.class));
        verify(auditLogEventPublisher).publishLogEvent(eq(null), eq("RESET PASSWORD"), eq("John Doe did a password reset"), eq("USER"), eq(request));
    }

    @Test
    void testResetPassword_userNotFound() {
        String token = "invalidToken";
        ResetPasswordDto dto = new ResetPasswordDto("newPassword123", "newPassword123");
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(jwtService.decodeToken(token)).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class, () -> {
            authenticationService.resetPassword(token, dto, request);
        });

        assertEquals("User not found", thrown.getMessage());
    }

    @Test
    void testResetPassword_passwordMismatch() {
        String token = "validToken";
        ResetPasswordDto dto = new ResetPasswordDto("newPassword123", "differentPassword123");
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User("John Doe", "john.doe@example.com", "oldPassword", UserRole.USER, "1990-01-01", true);

        when(jwtService.decodeToken(token)).thenReturn("john.doe@example.com");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        PasswordMismatchException thrown = assertThrows(PasswordMismatchException.class, () -> {
            authenticationService.resetPassword(token, dto, request);
        });

        assertEquals("Passwords does not match", thrown.getMessage());
    }
}
