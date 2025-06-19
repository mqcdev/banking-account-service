package com.nttdata.banking.bankaccount.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.nttdata.banking.bankaccount.application.BankAccountService;
import com.nttdata.banking.bankaccount.consumer.mapper.BalanceBankAccountModel;
import com.nttdata.banking.bankaccount.model.BankAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Class BankAccountConsumer.
 * BankAccount microservice class BankAccountConsumer.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BankAccountConsumer {

    @Autowired
    private BankAccountService bankAccountService;

    @KafkaListener(topics = "${spring.kafka.topic.bank.name}")
    public void listener(@Payload BalanceBankAccountModel balanceModel) {
        log.info("Message received : {} ", balanceModel);
        applyBalance(balanceModel).block();
    }

    private Mono<BankAccount> applyBalance(BalanceBankAccountModel request) {
        log.info("applyBalance executed : {} ", request);
        return bankAccountService.updateBalanceById(
                request.getIdBankAccount(), request.getBalance());
    }
}
