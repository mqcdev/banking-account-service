package com.nttdata.banking.account.services.impl;

import com.nttdata.banking.account.clients.CustomerServiceClient;
import com.nttdata.banking.account.models.BankAccount;
import com.nttdata.banking.account.models.Transaction;
import com.nttdata.banking.account.repositories.BankAccountRepository;
import com.nttdata.banking.account.repositories.TransactionRepository;
import com.nttdata.banking.account.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final BankAccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerServiceClient customerServiceClient;

    @Override
    public Mono<BankAccount> getAccountById(String id) {
        return accountRepository.findById(id);
    }

    @Override
    public Flux<BankAccount> getAccountsByClientId(String clientId) {
        return accountRepository.findByClientId(clientId);
    }

    @Override
    public Mono<BankAccount> createSavingsAccount(BankAccount account) {
        account.setType(BankAccount.AccountType.SAVINGS);

        return canClientHaveSavingsAccount(account.getClientId())
                .flatMap(canHave -> {
                    if (!canHave) {
                        return Mono.error(new RuntimeException("El cliente no puede tener cuentas de ahorro"));
                    }

                    return hasReachedAccountLimit(account.getClientId(), BankAccount.AccountType.SAVINGS);
                })
                .flatMap(hasReached -> {
                    if (hasReached) {
                        return Mono.error(new RuntimeException("El cliente ya tiene una cuenta de ahorro"));
                    }

                    return initializeAndSaveAccount(account);
                });
    }

    @Override
    public Mono<BankAccount> createCheckingAccount(BankAccount account) {
        account.setType(BankAccount.AccountType.CHECKING);

        return canClientHaveCheckingAccount(account.getClientId())
                .flatMap(canHave -> {
                    if (!canHave) {
                        return Mono.error(new RuntimeException("El cliente no puede tener cuentas corrientes"));
                    }

                    return customerServiceClient.getClientById(account.getClientId());
                })
                .flatMap(client -> {
                    if (!client.canHaveMultipleCheckingAccounts()) {
                        return hasReachedAccountLimit(account.getClientId(), BankAccount.AccountType.CHECKING)
                                .flatMap(hasReached -> {
                                    if (hasReached) {
                                        return Mono.error(new RuntimeException("El cliente personal solo puede tener una cuenta corriente"));
                                    }
                                    return Mono.just(true);
                                });
                    }
                    return Mono.just(true);
                })
                .flatMap(ignored -> initializeAndSaveAccount(account));
    }

    @Override
    public Mono<BankAccount> createFixedTermAccount(BankAccount account) {
        account.setType(BankAccount.AccountType.FIXED_TERM);

        return canClientHaveFixedTermAccount(account.getClientId())
                .flatMap(canHave -> {
                    if (!canHave) {
                        return Mono.error(new RuntimeException("El cliente no puede tener cuentas a plazo fijo"));
                    }

                    return hasReachedAccountLimit(account.getClientId(), BankAccount.AccountType.FIXED_TERM);
                })
                .flatMap(hasReached -> {
                    if (hasReached) {
                        return Mono.error(new RuntimeException("El cliente ya tiene una cuenta a plazo fijo"));
                    }

                    return initializeAndSaveAccount(account);
                });
    }

    private Mono<BankAccount> initializeAndSaveAccount(BankAccount account) {
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        account.setActive(true);

        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }

        return accountRepository.save(account);
    }

    @Override
    public Mono<BankAccount> updateAccount(BankAccount account) {
        return getAccountById(account.getId())
                .flatMap(existingAccount -> {
                    account.setUpdatedAt(LocalDateTime.now());
                    return accountRepository.save(account);
                });
    }

    @Override
    public Mono<Void> deleteAccount(String id) {
        return accountRepository.deleteById(id);
    }

    @Override
    public Mono<BigDecimal> getBalance(String accountId) {
        return getAccountById(accountId)
                .map(BankAccount::getBalance);
    }

    @Override
    public Mono<Boolean> validateAccountOwnership(String accountId, String clientId) {
        return getAccountById(accountId)
                .map(account -> account.getClientId().equals(clientId));
    }

    @Override
    public Mono<Boolean> validateAccountMovement(String accountId, BigDecimal amount, String movementType) {
        Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(movementType);

        return getAccountById(accountId)
                .flatMap(account -> {
                    if (transactionType == Transaction.TransactionType.WITHDRAWAL && account.getBalance().compareTo(amount) < 0) {
                        return Mono.just(false); // Saldo insuficiente
                    }

                    Integer limit = account.getMonthlyTransactionLimit();
                    if (limit != null) {
                        // Contar transacciones del mes
                        // Lógica para contar transacciones mensuales
                        return Mono.just(true); // Simplificado para este ejemplo
                    }

                    return Mono.just(true);
                });
    }

    @Override
    public Mono<Boolean> validateTransactionAllowed(String accountId, LocalDate date) {
        return getAccountById(accountId)
                .map(account -> account.validateMovementDate(date));
    }

    @Override
    public Flux<Transaction> getAccountTransactions(String accountId) {
        return transactionRepository.findBySourceAccountIdOrDestinationAccountId(accountId, accountId);
    }

    @Override
    public Mono<Boolean> canClientHaveSavingsAccount(String clientId) {
        return customerServiceClient.getClientById(clientId)
                .map(client -> client.canHaveSavingsAccount());
    }

    @Override
    public Mono<Boolean> canClientHaveFixedTermAccount(String clientId) {
        return customerServiceClient.getClientById(clientId)
                .map(client -> client.canHaveFixedTermAccount());
    }

    @Override
    public Mono<Boolean> canClientHaveCheckingAccount(String clientId) {
        return customerServiceClient.getClientById(clientId)
                .map(client -> client.isPersonal() || client.isBusiness());
    }

    @Override
    public Mono<Boolean> hasReachedAccountLimit(String clientId, BankAccount.AccountType accountType) {
        return customerServiceClient.getClientById(clientId)
                .flatMap(client -> {
                    if (client.isBusiness() && client.canHaveMultipleCheckingAccounts() &&
                            accountType == BankAccount.AccountType.CHECKING) {
                        return Mono.just(false); // Los clientes empresariales pueden tener múltiples cuentas corrientes
                    }

                    // Para otros tipos de cuenta o clientes personales, verificar límites
                    return accountRepository.countByClientIdAndType(clientId, accountType)
                            .map(count -> count > 0);
                });
    }
}