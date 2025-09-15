package app.tradeflows.api.user_service.controllers;

import app.tradeflows.api.user_service.dtos.ResetPasswordDto;
import app.tradeflows.api.user_service.dtos.LoginUserDto;
import app.tradeflows.api.user_service.dtos.RegisterUserDto;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.exceptions.ConflictException;
import app.tradeflows.api.user_service.exceptions.InvalidCredentialExcetion;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.exceptions.PasswordMismatchException;
import app.tradeflows.api.user_service.responses.LoginResponse;
import app.tradeflows.api.user_service.services.AuthenticationService;
import app.tradeflows.api.user_service.services.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

@Controller
@RestController
@RequestMapping(value = "/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService,
                                    AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(security = {})
    @SecurityRequirements(value = {})
    public ResponseEntity<User> register(@RequestBody RegisterUserDto registerUserDto, HttpServletRequest request) throws ConflictException, MessagingException, UnsupportedEncodingException {
        User registeredUser = authenticationService.registerUser(registerUserDto, request);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(security = {})
    @SecurityRequirements(value = {})
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserDto loginUserDto, HttpServletRequest request) throws InvalidCredentialExcetion, ConflictException {
        return ResponseEntity.ok(authenticationService.authenticate(loginUserDto, request));
    }

    @PatchMapping(value = "/verify/{token}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(security = {})
    @SecurityRequirements(value = {})
    public void verifyUser(@PathVariable String token, HttpServletRequest request)
            throws ConflictException, NotFoundException {
        authenticationService.confirmUser(token, request);
    }

    @PatchMapping("/forgot-password/{email}")
    @Operation(security = {})
    @SecurityRequirements(value = {})
    public void forgotPassword(@PathVariable String email, HttpServletRequest request)
            throws NotFoundException, MessagingException, UnsupportedEncodingException {
        authenticationService.forgotPassword(email, request);
    }

    @PatchMapping("/reset-password/{token}")
    @Operation(security = {})
    @SecurityRequirements(value = {})
    public void resetPassword(@RequestBody ResetPasswordDto resetPasswordDTO, @PathVariable String token, HttpServletRequest request)
            throws NotFoundException, PasswordMismatchException {
        authenticationService.resetPassword(token, resetPasswordDTO, request);
    }
}
