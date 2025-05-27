package com.nttdata.banking.account.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BankingException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBankingException(
            BankingException ex, ServerWebExchange exchange) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(exchange.getRequest().getPath().value())
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
    }

    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleRuntimeException(
            RuntimeException ex, ServerWebExchange exchange) {

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(exchange.getRequest().getPath().value())
                .build();

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
    }
}