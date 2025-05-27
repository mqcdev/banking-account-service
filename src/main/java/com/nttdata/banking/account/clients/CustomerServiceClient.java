package com.nttdata.banking.account.clients;

import com.nttdata.banking.account.dto.ClientDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerServiceClient {
    private final WebClient webClient;

    public CustomerServiceClient(@Value("${services.customer.service-name}") String serviceName,
                                 WebClient.Builder loadBalancedWebClientBuilder) {
        this.webClient = loadBalancedWebClientBuilder
                .baseUrl("http://" + serviceName) // Usar el nombre del servicio para discovery
                .build();
    }

    public Mono<ClientDTO> getClientById(String clientId) {
        return webClient.get()
                .uri("/api/v1/customers/{id}", clientId)
                .retrieve()
                .bodyToMono(ClientDTO.class)
                .onErrorResume(e -> {
                    // Manejo de errores
                    return Mono.error(e);
                });
    }
}