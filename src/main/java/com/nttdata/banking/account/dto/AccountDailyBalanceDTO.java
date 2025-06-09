package com.nttdata.banking.account.dto;

import com.nttdata.banking.account.models.BankAccount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDailyBalanceDTO {
    private String accountId;
    private String accountNumber;
    private String clientId;
    private BankAccount.AccountType type;
    private String date;
    private BigDecimal balance;
}