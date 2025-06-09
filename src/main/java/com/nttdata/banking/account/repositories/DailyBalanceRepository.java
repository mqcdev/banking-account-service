package com.nttdata.banking.account.repositories;

import com.nttdata.banking.account.models.AccountDailyBalance;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Repository
public interface DailyBalanceRepository extends ReactiveMongoRepository<AccountDailyBalance, String> {
    Flux<AccountDailyBalance> findByClientIdAndDateBetween(String clientId, LocalDate startDate, LocalDate endDate);
    Flux<AccountDailyBalance> findByAccountIdAndDateBetween(String accountId, LocalDate startDate, LocalDate endDate);
}