package app.tradeflows.api.user_service.services;

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
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AccountService accountService;
    private final JwtService jwtService;
    private final EmailEventPublisher emailEventPublisher;
    private final AuditLogEventPublisher auditLogEventPublisher;
    private final AccountCreationEventPublisher accountCreationEventPublisher;

    @Transactional
    public User registerUser(RegisterUserDto request, HttpServletRequest httpServletRequest) throws ConflictException {
        logger.info("Registering user with email address: {}", request.getEmail());
        Optional<User> isEmailExisting = userRepository.findByEmail(request.getEmail());
        if (isEmailExisting.isPresent()) {
            throw new ConflictException("Email already exists");
        }

        User user = userRepository.save(new User(request.getName(), request.getEmail(), passwordEncoder.encode(request.getPassword()),
                UserRole.USER, request.getDob(), false));
        logger.info("Generating confirmation token for user: {}", user.getEmail());
        String token = jwtService.generateToken(user);
        Context context = new Context();
        context.setVariable("action_url", "https://tradeflows.app/email-verification?token="+token);
//        emailEventPublisher.publishEmailEvent(user.getEmail(), "Verify Your Email Address", "emailConfirmation", context);
        auditLogEventPublisher.publishLogEvent(user.getId(), "REGISTER", user.getName()+" Registered an account", String.valueOf(user.getRole()), httpServletRequest);
        return user;
    }

    public LoginResponse authenticate(LoginUserDto input, HttpServletRequest request) throws InvalidCredentialExcetion, ConflictException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        User authenticatedUser = userRepository.findByEmail(input.getEmail()).orElseThrow(() -> new InvalidCredentialExcetion("User/password invalid"));

        String jwtToken = jwtService.generateToken(authenticatedUser);
//        auditLogEventPublisher.publishLogEvent(authenticatedUser.getId(), "LOGIN", authenticatedUser.getName() + " logged to their account", String.valueOf(authenticatedUser.getRole()), request);
        accountCreationEventPublisher.publishEvent(authenticatedUser, request);
        return new LoginResponse().setToken(jwtToken).setExpiresIn(jwtService.getExpirationTime());
    }

    @Transactional
    public void confirmUser(String token, HttpServletRequest request)
            throws ConflictException, NotFoundException {
        String email = jwtService.decodeToken(token);
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            throw new NotFoundException("User does not exists");
        }

        if (user.get().isEnabled()) {
            logger.info("User already enabled");
            return;
        }

        var updatedUser = user.get();
        updatedUser.setIsActive();

        userRepository.save(updatedUser);
        auditLogEventPublisher.publishLogEvent(updatedUser.getId(), "CONFIRM EMAIL", updatedUser.getName()+" confirm to their account", String.valueOf(updatedUser.getRole()), request);

        accountService.createAccountByUserEmail(updatedUser, request);
    }

    @Transactional
    public void forgotPassword(String email, HttpServletRequest request) throws NotFoundException, MessagingException, UnsupportedEncodingException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        logger.info("Generating reset token for user: {}", user.getEmail());
        // Generate token to expire in 60 minutes
        String token = jwtService.generateToken(user, 60 * 60 * 60);
        Context context = new Context();
        context.setVariable("action_url", "https://tradeflows.app/reset-password?token="+token);
        emailEventPublisher.publishEmailEvent(user.getEmail(), "Reset your password", "passwordResetEmail", context);
        auditLogEventPublisher.publishLogEvent(user.getId(), "FORGOT PASSWORD", user.getName()+" forgot their password", String.valueOf(user.getRole()), request);

    }

    public void resetPassword(String token, ResetPasswordDto dto, HttpServletRequest request)
            throws NotFoundException, PasswordMismatchException {
        String email = jwtService.decodeToken(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!Objects.equals(dto.getNewPassword(), dto.getConfirmPassword()))
            throw new PasswordMismatchException("Passwords does not match");

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        auditLogEventPublisher.publishLogEvent(user.getId(), "RESET PASSWORD", user.getName()+" did a password reset", String.valueOf(user.getRole()), request);
    }
}
