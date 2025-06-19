package com.nttdata.banking.bankaccount.consumer.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

/**
 * Class BankAccount.
 * BankAccount microservice class BalanceBankAccountModel.
 */
@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class BalanceBankAccountModel {

    @JsonIgnore
    private String id;

    private String idBankAccount;

    private Double balance;
}
