package com.nttdata.banking.account.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String sourceAccountId;
    private String destinationAccountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private LocalDateTime transactionDate;
    private BigDecimal balanceAfter;
    private String reference;
    private Boolean isCredit;

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER, PAYMENT, CHARGE
    }
}