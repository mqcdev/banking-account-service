package com.nttdata.banking.account.services.impl;

import com.nttdata.banking.account.clients.CreditServiceClient;
import com.nttdata.banking.account.clients.CustomerServiceClient;
import com.nttdata.banking.account.dto.ClientDTO;
import com.nttdata.banking.account.dto.CommissionDTO;
import com.nttdata.banking.account.exceptions.BankingException;
import com.nttdata.banking.account.models.AccountDailyBalance;
import com.nttdata.banking.account.models.BankAccount;
import com.nttdata.banking.account.models.Transaction;
import com.nttdata.banking.account.repositories.BankAccountRepository;
import com.nttdata.banking.account.repositories.CommissionRepository;
import com.nttdata.banking.account.repositories.DailyBalanceRepository;
import com.nttdata.banking.account.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import com.nttdata.banking.account.models.Commission;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final BankAccountRepository accountRepository;
    private final CustomerServiceClient customerServiceClient;
    private final CreditServiceClient creditServiceClient;
    private final DailyBalanceRepository dailyBalanceRepository;
    private final CommissionRepository commissionRepository;

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
    public Mono<BankAccount> createVipAccount(BankAccount account) {
        account.setType(BankAccount.AccountType.SAVINGS);
        account.setProfileType(ClientDTO.ClientType.VIP);

        return validateCreditCardRequirement(account.getClientId())
                .flatMap(hasCard -> {
                    if (!hasCard) {
                        return Mono.error(new BankingException("CREDIT_CARD_REQUIRED",
                                "Para crear una cuenta VIP, el cliente debe tener una tarjeta de crédito"));
                    }

                    return validateMinimumOpeningAmount(account)
                            .flatMap(isValid -> {
                                if (!isValid) {
                                    return Mono.error(new BankingException("MINIMUM_AMOUNT_REQUIRED",
                                            "El monto mínimo de apertura es requerido"));
                                }

                                return initializeAndSaveAccount(account);
                            });
                });
    }

    @Override
    public Mono<BankAccount> createPymeAccount(BankAccount account) {
        account.setType(BankAccount.AccountType.CHECKING);
        account.setProfileType(ClientDTO.ClientType.PYME);
        account.setMaintenanceFee(BigDecimal.ZERO); // Sin comisión de mantenimiento

        return validateCreditCardRequirement(account.getClientId())
                .flatMap(hasCard -> {
                    if (!hasCard) {
                        return Mono.error(new BankingException("CREDIT_CARD_REQUIRED",
                                "Para crear una cuenta PYME, el cliente debe tener una tarjeta de crédito"));
                    }

                    return validateMinimumOpeningAmount(account)
                            .flatMap(isValid -> {
                                if (!isValid) {
                                    return Mono.error(new BankingException("MINIMUM_AMOUNT_REQUIRED",
                                            "El monto mínimo de apertura es requerido"));
                                }

                                return initializeAndSaveAccount(account);
                            });
                });
    }

    @Override
    public Mono<Boolean> validateMinimumOpeningAmount(BankAccount account) {
        if (account.getMinimumOpeningAmount() == null) {
            return Mono.just(true); // No hay monto mínimo configurado
        }

        if (account.getBalance() == null || account.getBalance().compareTo(account.getMinimumOpeningAmount()) < 0) {
            return Mono.just(false); // No cumple el monto mínimo
        }

        return Mono.just(true);
    }

    @Override
    public Mono<Boolean> validateCreditCardRequirement(String clientId) {
        return creditServiceClient.hasClientCreditCard(clientId)
                .onErrorResume(e -> Mono.just(false)); // En caso de error, asumimos que no tiene tarjeta
    }

    @Override
    public Mono<BigDecimal> calculateAverageDailyBalance(String accountId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        return dailyBalanceRepository.findByAccountIdAndDateBetween(accountId, startDate, endDate)
                .map(AccountDailyBalance::getBalance)
                .collectList()
                .map(balances -> {
                    if (balances.isEmpty()) {
                        return BigDecimal.ZERO;
                    }

                    BigDecimal total = balances.stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return total.divide(BigDecimal.valueOf(balances.size()), 2, RoundingMode.HALF_UP);
                });
    }

    @Override
    public Flux<CommissionDTO> getCommissionsByPeriod(String accountId, LocalDate startDate, LocalDate endDate) {
        return commissionRepository.findByAccountIdAndDateBetween(accountId, startDate, endDate)
                .map(commission -> CommissionDTO.builder()
                        .accountId(commission.getAccountId())
                        .accountNumber(commission.getAccountNumber())
                        .clientId(commission.getClientId())
                        .type(commission.getAccountType())
                        .date(commission.getDate().toString())
                        .commissionType(commission.getCommissionType())
                        .amount(commission.getAmount())
                        .build());
    }

    @Override
    public Mono<Map<String, BigDecimal>> getAverageDailyBalanceSummary(String clientId, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        return accountRepository.findByClientId(clientId)
                .flatMap(account -> calculateAverageDailyBalance(account.getId(), month)
                        .map(avgBalance -> Tuples.of(account.getAccountNumber(), avgBalance)))
                .collectMap(tuple -> tuple.getT1(), tuple -> tuple.getT2());
    }


    @Override
    public Mono<Boolean> validateAccountMovement(String accountId, BigDecimal amount, String movementType) {
        Transaction.TransactionType transactionType = Transaction.TransactionType.valueOf(movementType);

        // Agregar validación de límite de transacciones gratuitas
        // Lógica existente...
        return getAccountById(accountId)
                .flatMap(account -> {
                    // Aquí iría la validación del movimiento según las reglas de negocio
                    // Por ejemplo, verificar si hay suficiente saldo para un retiro

                    // Agregamos validación de límite de transacciones gratuitas
                    if ("WITHDRAWAL".equals(movementType) || "DEPOSIT".equals(movementType)) {
                        return getMonthlyTransactionCount(accountId)
                                .map(count -> {
                                    // Si hay límite de transacciones gratuitas y lo excede
                                    if (account.getFreeTransactionsLimit() != null &&
                                            count >= account.getFreeTransactionsLimit()) {
                                        // Aquí se podría guardar una comisión, pero el movimiento es válido
                                        return true;
                                    }
                                    return true;
                                });
                    }

                    return Mono.just(true);
                });

    }

    // Método auxiliar para contar transacciones del mes
    private Mono<Long> getMonthlyTransactionCount(String accountId) {
        // Implementación: contar transacciones del mes actual para la cuenta
        LocalDate start = YearMonth.now().atDay(1);
        LocalDate end = YearMonth.now().atEndOfMonth();

        // Esta llamada usaría el microservicio de transacciones
        // o un repositorio local si se guardan las transacciones en este servicio
        return Mono.just(0L); // Simplificado para el ejemplo
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