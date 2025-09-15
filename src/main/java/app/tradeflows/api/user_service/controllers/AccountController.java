package app.tradeflows.api.user_service.controllers;

import app.tradeflows.api.user_service.dtos.AccountTopUpDto;
import app.tradeflows.api.user_service.entities.Account;
import app.tradeflows.api.user_service.entities.User;
import app.tradeflows.api.user_service.exceptions.NotFoundException;
import app.tradeflows.api.user_service.services.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RequestMapping("/api/accounts")
@RestController
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService){
        this.accountService = accountService;
    }

    @GetMapping(value = "/{userId}")
    public ResponseEntity<Account> getUserAccountByUserId(@PathVariable String userId) throws NotFoundException {
        Account account = accountService.getUserAccountByUserId(userId);
        return ResponseEntity.ok(account);
    }

    @PostMapping(value = "/top-up")
    public ResponseEntity<Void> topUpAccount(@RequestBody AccountTopUpDto dto, Authentication authentication, HttpServletRequest httpServletRequest) throws NotFoundException {
        accountService.topUpAccount(dto, httpServletRequest);
        return ResponseEntity.accepted().build();
    }
}
