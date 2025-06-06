package com.nttdata.banking.account.controllers;

import com.nttdata.banking.account.clients.CustomerServiceClient;
import com.nttdata.banking.account.models.BankAccount;
import com.nttdata.banking.account.dto.ClientDTO;
import com.nttdata.banking.account.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final CustomerServiceClient customerServiceClient;

    // Endpoint para consultar información del cliente
    @GetMapping("/client-info/{clientId}")
    public Mono<ClientDTO> getClientInfo(@PathVariable String clientId) {
        return customerServiceClient.getClientById(clientId);
    }

    // Endpoints de validación
    @GetMapping("/can-have-savings/{clientId}")
    public Mono<Boolean> canHaveSavingsAccount(@PathVariable String clientId) {
        return accountService.canClientHaveSavingsAccount(clientId);
    }

    @GetMapping("/can-have-fixed-term/{clientId}")
    public Mono<Boolean> canHaveFixedTermAccount(@PathVariable String clientId) {
        return accountService.canClientHaveFixedTermAccount(clientId);
    }

    @GetMapping("/can-have-checking/{clientId}")
    public Mono<Boolean> canHaveCheckingAccount(@PathVariable String clientId) {
        return accountService.canClientHaveCheckingAccount(clientId);
    }

    @GetMapping("/has-reached-limit/{clientId}")
    public Mono<Boolean> hasReachedAccountLimit(
            @PathVariable String clientId,
            @RequestParam BankAccount.AccountType accountType,
            @RequestParam ClientDTO.ClientType profileType) {
        return accountService.hasReachedAccountLimit(clientId, accountType, profileType);
    }
    // Endpoints existentes
    @GetMapping("/{id}")
    public Mono<BankAccount> getAccount(@PathVariable String id) {
        return accountService.getAccountById(id);
    }

    @GetMapping("/client/{clientId}")
    public Flux<BankAccount> getAccountsByClient(@PathVariable String clientId) {
        return accountService.getAccountsByClientId(clientId);
    }
    @GetMapping("/client/filter/{accountNumber}")
    public Mono<BankAccount> getAccountsByAccountNumber(@PathVariable String accountNumber) {
        return accountService.getAccountByAccountNumber(accountNumber);
    }

    @PostMapping("/savings")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BankAccount> createSavingsAccount(@RequestBody BankAccount account) {
        return accountService.createSavingsAccount(account);
    }

    @PostMapping("/checking")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BankAccount> createCheckingAccount(@RequestBody BankAccount account) {
        return accountService.createCheckingAccount(account);
    }

    @PostMapping("/fixed-term")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BankAccount> createFixedTermAccount(@RequestBody BankAccount account) {
        return accountService.createFixedTermAccount(account);
    }
    @PutMapping("/{id}")
    public Mono<BankAccount> updateAccount(@PathVariable String id, @RequestBody BankAccount account) {
        account.setId(id);
        return accountService.updateAccount(account);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteAccount(@PathVariable String id) {
        return accountService.deleteAccount(id);
    }

    @GetMapping("/{id}/balance")
    public Mono<BigDecimal> getBalance(@PathVariable String id) {
        return accountService.getBalance(id);
    }

    @GetMapping("/{id}/validate-movement")
    public Mono<Boolean> validateMovement(
            @PathVariable String id,
            @RequestParam BigDecimal amount,
            @RequestParam String movementType) {
        return accountService.validateAccountMovement(id, amount, movementType);
    }

}