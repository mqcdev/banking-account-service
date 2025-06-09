package com.nttdata.banking.account.repositories;

import com.nttdata.banking.account.models.Commission;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Repository
public interface CommissionRepository extends ReactiveMongoRepository<Commission, String> {
    Flux<Commission> findByClientIdAndDateBetween(String clientId, LocalDate startDate, LocalDate endDate);
    Flux<Commission> findByAccountIdAndDateBetween(String accountId, LocalDate startDate, LocalDate endDate);
}