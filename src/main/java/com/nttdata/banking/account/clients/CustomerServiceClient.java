package com.nttdata.banking.account.clients;

import com.nttdata.banking.account.dto.ClientDTO;
import com.nttdata.banking.account.dto.ClientDTO.ClientType;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;

@Component
public class CustomerServiceClient {
    private final WebClient webClient;
    private static final boolean USE_MOCK = true; // En producción esto será false

    public CustomerServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://customer-service") // URL real del servicio de clientes
                .build();
    }

    public Mono<ClientDTO> getClientById(String clientId) {
        if (USE_MOCK) {
            return Mono.just(getMockClient(clientId));
        }

        return webClient.get()
                .uri("/clients/{id}", clientId)
                .retrieve()
                .bodyToMono(ClientDTO.class)
                .onErrorResume(e -> {
                    // Manejo de errores, en caso de fallo se devuelve un cliente mock
                    return Mono.just(getMockClient(clientId));
                });
    }

    // Método mock para simular respuestas del servicio de clientes
    private ClientDTO getMockClient(String clientId) {
        // Ejemplos de clientes para pruebas
        switch (clientId) {
            case "CLI-P001": // Cliente personal
                return ClientDTO.builder()
                        .id(clientId)
                        .name("Juan Pérez")
                        .documentType("DNI")
                        .documentNumber("45896325")
                        .email("juan.perez@example.com")
                        .phone("987654321")
                        .address("Av. Principal 123")
                        .types(Collections.singletonList(ClientType.PERSONAL))
                        .build();

            case "CLI-E001": // Cliente empresarial
                return ClientDTO.builder()
                        .id(clientId)
                        .name("Empresa ABC")
                        .documentType("RUC")
                        .documentNumber("20123456789")
                        .email("contacto@empresaabc.com")
                        .phone("912345678")
                        .address("Av. Industrial 456")
                        .types(Collections.singletonList(ClientType.BUSINESS))
                        .build();

            case "CLI-PE001": // Cliente con ambos tipos
                return ClientDTO.builder()
                        .id(clientId)
                        .name("Roberto González")
                        .documentType("DNI")
                        .documentNumber("12345678")
                        .email("roberto@example.com")
                        .phone("987123456")
                        .address("Calle Las Flores 789")
                        .types(Arrays.asList(ClientType.PERSONAL, ClientType.BUSINESS))
                        .build();

            case "CLI-P002": // Otro cliente personal
                return ClientDTO.builder()
                        .id(clientId)
                        .name("María López")
                        .documentType("DNI")
                        .documentNumber("56789012")
                        .email("maria@example.com")
                        .phone("956782345")
                        .address("Jr. Los Olivos 567")
                        .types(Collections.singletonList(ClientType.PERSONAL))
                        .build();

            case "CLI-P003": // Firmante autorizado
                return ClientDTO.builder()
                        .id(clientId)
                        .name("Carlos Mendoza")
                        .documentType("DNI")
                        .documentNumber("34567890")
                        .email("carlos@example.com")
                        .phone("945678912")
                        .address("Av. Los Pinos 234")
                        .types(Collections.singletonList(ClientType.PERSONAL))
                        .build();

            case "CLI-P004": // Otro firmante autorizado
                return ClientDTO.builder()
                        .id(clientId)
                        .name("Ana Silva")
                        .documentType("DNI")
                        .documentNumber("67890123")
                        .email("ana@example.com")
                        .phone("912378945")
                        .address("Jr. Las Palmeras 567")
                        .types(Collections.singletonList(ClientType.PERSONAL))
                        .build();

            case "CLI-E002": // Otra empresa
                return ClientDTO.builder()
                        .id(clientId)
                        .name("Empresa XYZ")
                        .documentType("RUC")
                        .documentNumber("20987654321")
                        .email("contacto@empresaxyz.com")
                        .phone("943215678")
                        .address("Av. Los Negocios 789")
                        .types(Collections.singletonList(ClientType.BUSINESS))
                        .build();

            default:
                // Cliente por defecto (personal)
                return ClientDTO.builder()
                        .id(clientId)
                        .name("Cliente Genérico")
                        .documentType("DNI")
                        .documentNumber("00000000")
                        .email("cliente@example.com")
                        .phone("900000000")
                        .address("Sin dirección")
                        .types(Collections.singletonList(ClientType.PERSONAL))
                        .build();
        }
    }
}