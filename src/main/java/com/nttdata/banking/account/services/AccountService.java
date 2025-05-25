package com.nttdata.banking.account.services;

import com.nttdata.banking.account.models.BankAccount;
import com.nttdata.banking.account.models.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AccountService {
    Mono<BankAccount> getAccountById(String id);
    Flux<BankAccount> getAccountsByClientId(String clientId);
    Mono<BankAccount> createSavingsAccount(BankAccount account);
    Mono<BankAccount> createCheckingAccount(BankAccount account);
    Mono<BankAccount> createFixedTermAccount(BankAccount account);
    Mono<BankAccount> updateAccount(BankAccount account);
    Mono<Void> deleteAccount(String id);
    Mono<BigDecimal> getBalance(String accountId);
    Mono<Boolean> validateAccountOwnership(String accountId, String clientId);
    Mono<Boolean> validateAccountMovement(String accountId, BigDecimal amount, String movementType);
    Mono<Boolean> validateTransactionAllowed(String accountId, LocalDate date);
    Flux<Transaction> getAccountTransactions(String accountId);

    // Métodos nuevos para validar reglas de negocio relacionadas con clientes
    Mono<Boolean> canClientHaveSavingsAccount(String clientId);
    Mono<Boolean> canClientHaveFixedTermAccount(String clientId);
    Mono<Boolean> canClientHaveCheckingAccount(String clientId);
    Mono<Boolean> hasReachedAccountLimit(String clientId, BankAccount.AccountType accountType);
}