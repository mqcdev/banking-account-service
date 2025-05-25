package com.nttdata.banking.account.controllers;

import com.nttdata.banking.account.clients.CustomerServiceClient;
import com.nttdata.banking.account.models.BankAccount;
import com.nttdata.banking.account.models.Transaction;
import com.nttdata.banking.account.dto.ClientDTO;
import com.nttdata.banking.account.services.AccountService;
import com.nttdata.banking.account.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;
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
            @RequestParam BankAccount.AccountType accountType) {
        return accountService.hasReachedAccountLimit(clientId, accountType);
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

    @GetMapping("/{id}/transactions")
    public Flux<Transaction> getAccountTransactions(@PathVariable String id) {
        return accountService.getAccountTransactions(id);
    }

    @GetMapping("/{id}/validate-movement")
    public Mono<Boolean> validateMovement(
            @PathVariable String id,
            @RequestParam BigDecimal amount,
            @RequestParam String movementType) {
        return accountService.validateAccountMovement(id, amount, movementType);
    }

    @GetMapping("/{id}/validate-transaction-date")
    public Mono<Boolean> validateTransactionDate(
            @PathVariable String id,
            @RequestParam(required = false) String dateStr) {
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        return accountService.validateTransactionAllowed(id, date);
    }
    @PostMapping("/{id}/deposit")
    public Mono<Transaction> deposit(
            @PathVariable String id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        return transactionService.createTransaction(
                id, null, Transaction.TransactionType.DEPOSIT, amount,
                description != null ? description : "Depósito");
    }

    @PostMapping("/{id}/withdrawal")
    public Mono<Transaction> withdrawal(
            @PathVariable String id,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        return transactionService.createTransaction(
                id, null, Transaction.TransactionType.WITHDRAWAL, amount,
                description != null ? description : "Retiro");
    }

    @PostMapping("/{sourceId}/transfer/{destinationId}")
    public Mono<Transaction> transfer(
            @PathVariable String sourceId,
            @PathVariable String destinationId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description) {
        return transactionService.createTransaction(
                sourceId, destinationId, Transaction.TransactionType.TRANSFER, amount,
                description != null ? description : "Transferencia");
    }
}