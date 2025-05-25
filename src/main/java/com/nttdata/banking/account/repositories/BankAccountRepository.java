package com.nttdata.banking.account.repositories;

import com.nttdata.banking.account.models.BankAccount;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BankAccountRepository extends ReactiveMongoRepository<BankAccount, String> {
    Flux<BankAccount> findByClientId(String clientId);
    Mono<Long> countByClientIdAndType(String clientId, BankAccount.AccountType type);
}