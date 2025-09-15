package app.tradeflows.api.user_service.services;

import app.tradeflows.api.user_service.configurations.JsonBuilder;
import app.tradeflows.api.user_service.dtos.ChangePasswordDto;
import app.tradeflows.api.user_service.dtos.RegisterUserDto;
import app.tradeflows.api.user_service.dtos.UserDTO;
import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.enums.UserRole;
import app.tradeflows.api.user_service.events.publishers.AuditLogEventPublisher;
import app.tradeflows.api.user_service.events.publishers.EmailEventPublisher;
import app.tradeflows.api.user_service.exceptions.BadRequestException;
import app.tradeflows.api.user_service.exceptions.ConflictException;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.exceptions.PasswordMismatchException;
import app.tradeflows.api.user_service.repositories.AccountRepository;
import app.tradeflows.api.user_service.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailEventPublisher emailEventPublisher;
    private final AuditLogEventPublisher auditLogEventPublisher;
    private final AccountService accountService;
    private final RedisService<Object> redisService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailEventPublisher emailEventPublisher, AuditLogEventPublisher auditLogEventPublisher, AccountService accountService, RedisService<Object> redisService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailEventPublisher = emailEventPublisher;
        this.auditLogEventPublisher = auditLogEventPublisher;
        this.accountService = accountService;
        this.redisService = redisService;
    }

    public List<User> allUsers() {
        return userRepository.findAll();
    }

    public UserDTO getLoggedInUser(User user) throws NotFoundException {
        var userDto = new UserDTO();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setDob(user.getDob());
        userDto.setRole(user.getRole());
        userDto.setActive(user.isActive());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());
       Account account = accountService.getUserAccountByUserId(user.getId());
       redisService.addItem(user.getId(), new JsonBuilder().gson().toJson(account));
       userDto.setAccount(account);
        return userDto;
    }

    public User getUserById(String id) throws NotFoundException {
        Optional<User> user = userRepository.findById(id);
        return user.orElseThrow(() -> new NotFoundException("User does not exist"));
    }

    public void changePassword(User user, ChangePasswordDto dto, HttpServletRequest httpServletRequest)
            throws BadRequestException, PasswordMismatchException {
        if (passwordEncoder.matches(dto.getChangePassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect password");
        }

        if (!Objects.equals(dto.getChangePassword(), dto.getConfirmChangePassword())) {
            throw new PasswordMismatchException("Passwords does not match");
        }

        user.setPassword(passwordEncoder.encode(dto.getChangePassword()));
        userRepository.save(user);
        auditLogEventPublisher.publishLogEvent(user.getId(), "PASSWORD_CHANGE", user.getName()+" Changed password", String.valueOf(user.getRole()), httpServletRequest);
    }

    @Transactional
    public void addAdminUser(RegisterUserDto request, HttpServletRequest httpServletRequest) throws ConflictException {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if(optionalUser.isPresent()){
            throw new ConflictException("User with email "+ request.getEmail()+ " already exists");
        }
        User user = new User(request.getName(), request.getEmail(), passwordEncoder.encode(request.getPassword()),
                UserRole.ADMIN, request.getDob(), true);
        userRepository.save(user);

        // Send email
        Context context = new Context();
        context.setVariable("name", request.getName());
        context.setVariable("username", request.getEmail());
        context.setVariable("password", request.getPassword());
        emailEventPublisher.publishEmailEvent(request.getEmail(), "Welcome to Trade flows! Your Account Details", "welcome", context);
        auditLogEventPublisher.publishLogEvent(user.getId(), "ADMIN_CREATE_USER", user.getName()+" Was created by admin", String.valueOf(user.getRole()), httpServletRequest);

    }
}
