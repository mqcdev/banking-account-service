package com.nttdata.banking.account.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "commissions")
public class Commission {
    @Id
    private String id;
    private String accountId;
    private String accountNumber;
    private String clientId;
    private BankAccount.AccountType accountType;
    private LocalDate date;
    private String commissionType; // MAINTENANCE, TRANSACTION
    private BigDecimal amount;
}