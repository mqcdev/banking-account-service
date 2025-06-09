package com.nttdata.banking.account.models;

import com.nttdata.banking.account.dto.ClientDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "accounts")
public class BankAccount {
    @Id
    private String id;
    private String accountNumber;
    private String clientId;
    private BigDecimal balance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;

    // Nuevo campo para especificar el perfil utilizado
    private ClientDTO.ClientType profileType;

    // Campos existentes
    private AccountType type;
    private Integer monthlyTransactions;
    private Integer maxMonthlyTransactions;
    private BigDecimal maintenanceFee;
    private List<String> accountHolders = new ArrayList<>();
    private List<String> authorizedSigners = new ArrayList<>();
    private Integer movementDay;
    private LocalDate maturityDate;
    private BigDecimal interestRate;

    // para proyecto 2 agrege
    private BigDecimal minimumOpeningAmount;
    private Integer freeTransactionsLimit;
    private BigDecimal transactionCommission;
    private BigDecimal minimumDailyBalance; // Para cuentas VIP
    // Tipos de cuenta
    public enum AccountType {
        SAVINGS, CHECKING, FIXED_TERM
    }

    // Métodos de negocio
    public BigDecimal getMaintenanceFee() {
        if (type == AccountType.CHECKING) {
            return maintenanceFee;
        }
        return BigDecimal.ZERO; // Cuentas de ahorro y plazo fijo no tienen comisión
    }

    public Integer getMonthlyTransactionLimit() {
        if (type == AccountType.SAVINGS) {
            return maxMonthlyTransactions;
        } else if (type == AccountType.FIXED_TERM) {
            return 1; // Cuentas a plazo fijo solo permiten 1 movimiento
        }
        return null; // Cuentas corrientes sin límite
    }

    public boolean validateClientEligibility(String clientId) {
        // En un caso real, se consultaría el tipo de cliente (personal o empresarial)
        // Para este ejemplo, asumimos que es válido
        return true;
    }

    public boolean validateMovementDate(LocalDate date) {
        if (type == AccountType.FIXED_TERM) {
            return date.getDayOfMonth() == movementDay;
        }
        return true; // Otros tipos de cuenta no tienen restricción de fecha
    }

    public void addAccountHolder(String clientId) {
        if (type == AccountType.CHECKING && !accountHolders.contains(clientId)) {
            accountHolders.add(clientId);
        }
    }

    public void addAuthorizedSigner(String clientId) {
        if (type == AccountType.CHECKING && !authorizedSigners.contains(clientId)) {
            authorizedSigners.add(clientId);
        }
    }
}