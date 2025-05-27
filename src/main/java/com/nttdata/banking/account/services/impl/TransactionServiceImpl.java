package com.nttdata.banking.account.services.impl;

import com.nttdata.banking.account.exceptions.BankingException;
import com.nttdata.banking.account.models.BankAccount;
import com.nttdata.banking.account.models.Transaction;
import com.nttdata.banking.account.repositories.BankAccountRepository;
import com.nttdata.banking.account.repositories.TransactionRepository;
import com.nttdata.banking.account.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final BankAccountRepository accountRepository;

    @Override
    public Mono<Transaction> createTransaction(String sourceAccountId, String destinationAccountId,
                                               Transaction.TransactionType type, BigDecimal amount,
                                               String description) {

        // Validar que la cantidad sea positiva
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.error(new BankingException("INVALID_AMOUNT", "El monto de la transacción debe ser mayor que cero"));
        }

        // Si es una transferencia, necesitamos actualizar ambas cuentas
        if (type == Transaction.TransactionType.TRANSFER && destinationAccountId != null) {
            // Primero verificar si la cuenta origen tiene fondos suficientes
            return accountRepository.findById(sourceAccountId)
                    .switchIfEmpty(Mono.error(new BankingException("ACCOUNT_NOT_FOUND", "La cuenta origen no existe")))
                    .flatMap(sourceAccount -> {
                        if (sourceAccount.getBalance().compareTo(amount) < 0) {
                            return Mono.error(new BankingException("INSUFFICIENT_FUNDS", "Saldo insuficiente para realizar el retiro"));
                        }

                        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(amount);
                        sourceAccount.setBalance(newSourceBalance);

                        return accountRepository.save(sourceAccount)
                                .then(accountRepository.findById(destinationAccountId))
                                .switchIfEmpty(Mono.error(new BankingException("ACCOUNT_NOT_FOUND", "La cuenta destino no existe")))
                                .flatMap(destinationAccount -> {
                                    BigDecimal newDestBalance = destinationAccount.getBalance().add(amount);
                                    destinationAccount.setBalance(newDestBalance);

                                    return accountRepository.save(destinationAccount)
                                            .then(Mono.defer(() -> {
                                                Transaction transaction = Transaction.builder()
                                                        .sourceAccountId(sourceAccountId)
                                                        .destinationAccountId(destinationAccountId)
                                                        .transactionType(type)
                                                        .amount(amount)
                                                        .description(description)
                                                        .transactionDate(LocalDateTime.now())
                                                        .balanceAfter(newSourceBalance)
                                                        .reference(UUID.randomUUID().toString())
                                                        .isCredit(false)
                                                        .build();

                                                return transactionRepository.save(transaction);
                                            }));
                                });
                    });
        } else {
            // Lógica para depósitos y retiros
            return accountRepository.findById(sourceAccountId)
                    .switchIfEmpty(Mono.error(new BankingException("ACCOUNT_NOT_FOUND", "La cuenta no existe")))
                    .flatMap(sourceAccount -> {
                        boolean isCredit = type == Transaction.TransactionType.DEPOSIT;

                        // Si es un retiro, verificar que tenga fondos suficientes
                        if (!isCredit && sourceAccount.getBalance().compareTo(amount) < 0) {
                            return Mono.error(new BankingException("INSUFFICIENT_FUNDS", "Saldo insuficiente para realizar el retiro"));
                        }

                        BigDecimal newBalance;
                        if (isCredit) {
                            newBalance = sourceAccount.getBalance().add(amount);
                        } else {
                            newBalance = sourceAccount.getBalance().subtract(amount);
                        }

                        sourceAccount.setBalance(newBalance);

                        return accountRepository.save(sourceAccount)
                                .then(Mono.defer(() -> {
                                    Transaction transaction = Transaction.builder()
                                            .sourceAccountId(sourceAccountId)
                                            .destinationAccountId(destinationAccountId)
                                            .transactionType(type)
                                            .amount(amount)
                                            .description(description)
                                            .transactionDate(LocalDateTime.now())
                                            .balanceAfter(newBalance)
                                            .reference(UUID.randomUUID().toString())
                                            .isCredit(isCredit)
                                            .build();

                                    return transactionRepository.save(transaction);
                                }));
                    });
        }
    }
}