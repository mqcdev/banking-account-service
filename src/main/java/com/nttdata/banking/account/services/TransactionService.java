package com.nttdata.banking.account.services;

import com.nttdata.banking.account.models.Transaction;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface TransactionService {
    Mono<Transaction> createTransaction(String sourceAccountId, String destinationAccountId,
                                        Transaction.TransactionType type, BigDecimal amount,
                                        String description);
}