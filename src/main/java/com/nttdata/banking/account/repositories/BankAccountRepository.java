package com.nttdata.banking.account.repositories;

import com.nttdata.banking.account.dto.ClientDTO.ClientType;
import com.nttdata.banking.account.models.BankAccount;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface BankAccountRepository extends ReactiveMongoRepository<BankAccount, String> {
    Flux<BankAccount> findByClientId(String clientId);
    Mono<BankAccount> findByAccountNumber(String accountNumber);
    Mono<Long> countByClientIdAndType(String clientId, BankAccount.AccountType type);
    Mono<Long> countByClientIdAndTypeAndProfileType(String clientId, BankAccount.AccountType type, ClientType profileType);
    // Para reportes de saldo promedio
    @Query("{ 'clientId': ?0, 'createdAt': { $lte: ?1 } }")
    Flux<BankAccount> findAccountsByClientAndCreatedBeforeDate(String clientId, LocalDateTime date);
}