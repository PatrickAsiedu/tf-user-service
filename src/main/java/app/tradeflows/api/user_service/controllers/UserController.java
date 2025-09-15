package app.tradeflows.api.user_service.controllers;

import app.tradeflows.api.user_service.dtos.ChangePasswordDto;
import app.tradeflows.api.user_service.dtos.RegisterUserDto;
import app.tradeflows.api.user_service.dtos.UserDTO;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.exceptions.BadRequestException;
import app.tradeflows.api.user_service.exceptions.ConflictException;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.exceptions.PasswordMismatchException;
import app.tradeflows.api.user_service.services.JwtService;
import app.tradeflows.api.user_service.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/users")
@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> authenticatedUser() throws NotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User currentUser = (User) authentication.getPrincipal();

        return ResponseEntity.ok(userService.getLoggedInUser(currentUser));
    }

    @GetMapping
    public ResponseEntity<List<User>> allUsers() {
        List <User> users = userService.allUsers();

        return ResponseEntity.ok(users);
    }

    @GetMapping(value = "/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id) throws NotFoundException {
        User user = userService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    @PatchMapping("/change-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void changePassword(@RequestBody ChangePasswordDto dto, HttpServletRequest httpServletRequest)
            throws PasswordMismatchException, BadRequestException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        userService.changePassword(currentUser, dto, httpServletRequest);
    }

    @PostMapping(value = "/admin", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void createAdmin(@RequestBody RegisterUserDto dto, HttpServletRequest request)
            throws ConflictException {
        userService.addAdminUser(dto, request);
    }
}