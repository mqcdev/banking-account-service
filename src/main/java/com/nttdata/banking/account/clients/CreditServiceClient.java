package com.nttdata.banking.account.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CreditServiceClient {
    private final WebClient webClient;

    public CreditServiceClient(@Value("${services.credit.service-name:banking-credit-service}") String serviceName,
                               WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("http://" + serviceName)
                .build();
    }

    public Mono<Boolean> hasClientCreditCard(String clientId) {
        return webClient.get()
                .uri("/api/v1/credits/customer/{customerId}", clientId)
                .retrieve()
                .bodyToFlux(Object.class)
                .hasElements()
                .onErrorResume(e -> Mono.just(false));
    }
}