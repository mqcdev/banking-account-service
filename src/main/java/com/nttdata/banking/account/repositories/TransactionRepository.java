package com.nttdata.banking.account.repositories;

import com.nttdata.banking.account.models.Transaction;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TransactionRepository extends ReactiveMongoRepository<Transaction, String> {
    Flux<Transaction> findBySourceAccountIdOrDestinationAccountId(String sourceAccountId, String destinationAccountId);
}