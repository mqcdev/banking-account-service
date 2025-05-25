package com.nttdata.banking.account.services.impl;

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

        return accountRepository.findById(sourceAccountId)
                .flatMap(sourceAccount -> {
                    boolean isCredit = type == Transaction.TransactionType.DEPOSIT;

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