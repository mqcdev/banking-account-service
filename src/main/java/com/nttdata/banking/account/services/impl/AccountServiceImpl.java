package com.nttdata.banking.account.services.impl;

import com.nttdata.banking.account.clients.CustomerServiceClient;
import com.nttdata.banking.account.dto.ClientDTO;
import com.nttdata.banking.account.exceptions.BankingException;
import com.nttdata.banking.account.models.BankAccount;
import com.nttdata.banking.account.models.Transaction;
import com.nttdata.banking.account.repositories.BankAccountRepository;
import com.nttdata.banking.account.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final BankAccountRepository accountRepository;
    private final CustomerServiceClient customerServiceClient;

    @Override
    public Mono<BankAccount> getAccountById(String id) {
        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new BankingException("ACCOUNT_NOT_FOUND", "No se encontró la cuenta con el ID proporcionado")));
    }
    @Override
    public Mono<BankAccount> getAccountByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .switchIfEmpty(Mono.error(new BankingException("ACCOUNT_NOT_FOUND", "No se encontró el numero de cuenta con el numero proporcionado")));
    }

    @Override
    public Flux<BankAccount> getAccountsByClientId(String clientId) {
        return accountRepository.findByClientId(clientId);
    }

    @Override
    public Mono<BankAccount> createSavingsAccount(BankAccount account) {
        account.setType(BankAccount.AccountType.SAVINGS);

        // Verificar que si el cliente tiene tipo PERSONAL
        return customerServiceClient.getClientById(account.getClientId())
                .flatMap(client -> {
                    // Verificar que el perfil seleccionado esté entre los tipos del cliente
                    if (!client.getCustomerType().contains(account.getProfileType())) {
                        return Mono.error(new BankingException("INVALID_PROFILE", "El perfil seleccionado no corresponde a los tipos disponibles para este cliente"));
                    }

                    // Verificar que sea perfil personal para cuentas de ahorro
                    if (account.getProfileType() != ClientDTO.ClientType.PERSONAL) {
                        return Mono.error(new BankingException("PROFILE_NOT_ALLOWED", "Solo los perfiles personales pueden tener cuentas de ahorro"));
                    }

                    return hasReachedAccountLimit(account.getClientId(), BankAccount.AccountType.SAVINGS, ClientDTO.ClientType.PERSONAL);
                })
                .flatMap(hasReached -> {
                    if (hasReached) {
                        return Mono.error(new BankingException("ACCOUNT_LIMIT_REACHED", "El cliente ya tiene una cuenta de ahorro"));
                    }
                    return initializeAndSaveAccount(account);
                });
    }

    @Override
    public Mono<BankAccount> createCheckingAccount(BankAccount account) {
        account.setType(BankAccount.AccountType.CHECKING);

        return customerServiceClient.getClientById(account.getClientId())
                .flatMap(client -> {
                    // Verificar que el perfil seleccionado esté entre los tipos del cliente
                    if (!client.getCustomerType().contains(account.getProfileType())) {
                        return Mono.error(new BankingException("INVALID_PROFILE", "El perfil seleccionado no corresponde a los tipos disponibles para este cliente"));
                    }

                    // Si es perfil personal, verificar límite de cuentas corrientes
                    if (account.getProfileType() == ClientDTO.ClientType.PERSONAL) {
                        return hasReachedAccountLimit(account.getClientId(), BankAccount.AccountType.CHECKING, ClientDTO.ClientType.PERSONAL)
                                .flatMap(hasReached -> {
                                    if (hasReached) {
                                        return Mono.error(new BankingException("ACCOUNT_LIMIT_REACHED", "El cliente personal solo puede tener una cuenta corriente"));
                                    }
                                    return Mono.just(true);
                                });
                    }

                    // Para perfil empresarial, puede tener múltiples cuentas corrientes
                    return Mono.just(true);
                })
                .flatMap(ignored -> initializeAndSaveAccount(account));
    }

    @Override
    public Mono<BankAccount> createFixedTermAccount(BankAccount account) {
        account.setType(BankAccount.AccountType.FIXED_TERM);

        return customerServiceClient.getClientById(account.getClientId())
                .flatMap(client -> {
                    // Verificar que el perfil seleccionado esté entre los tipos del cliente
                    if (!client.getCustomerType().contains(account.getProfileType())) {
                        return Mono.error(new BankingException("INVALID_PROFILE", "El perfil seleccionado no corresponde a los tipos disponibles para este cliente"));
                    }

                    // Solo perfiles personales pueden tener cuenta a plazo fijo
                    if (account.getProfileType() != ClientDTO.ClientType.PERSONAL) {
                        return Mono.error(new BankingException("PROFILE_NOT_ALLOWED", "Solo los perfiles personales pueden tener cuentas a plazo fijo"));
                    }

                    return hasReachedAccountLimit(account.getClientId(), BankAccount.AccountType.FIXED_TERM, ClientDTO.ClientType.PERSONAL);
                })
                .flatMap(hasReached -> {
                    if (hasReached) {
                        return Mono.error(new BankingException("ACCOUNT_LIMIT_REACHED", "El cliente ya tiene una cuenta a plazo fijo"));
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
                .map(account -> {
                    if (!account.getClientId().equals(clientId)) {
                        throw new BankingException("UNAUTHORIZED", "El cliente no es propietario de esta cuenta");
                    }
                    return true;
                });
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
    public Mono<Boolean> hasReachedAccountLimit(String clientId, BankAccount.AccountType accountType, ClientDTO.ClientType profileType) {
        if (profileType == ClientDTO.ClientType.BUSINESS && accountType == BankAccount.AccountType.CHECKING) {
            return Mono.just(false); // Los perfiles empresariales pueden tener múltiples cuentas corrientes
        }

        // Para perfiles personales, verificar límites
        return accountRepository.countByClientIdAndTypeAndProfileType(clientId, accountType, profileType)
                .map(count -> count > 0);
    }
}