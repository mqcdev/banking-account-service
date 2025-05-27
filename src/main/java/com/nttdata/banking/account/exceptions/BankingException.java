package com.nttdata.banking.account.exceptions;

import lombok.Getter;

@Getter
public class BankingException extends RuntimeException {
    private final String code;

    public BankingException(String message) {
        super(message);
        this.code = "BANKING_ERROR";
    }

    public BankingException(String code, String message) {
        super(message);
        this.code = code;
    }
}