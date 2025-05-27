package com.nttdata.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {
    private String id;
    private List<ClientType> customerType = new ArrayList<>();
    private String firstName;
    private String lastName;
    private String documentType;
    private String documentNumber;
    private String companyName;
    private String ruc;
    private String phoneNumber;
    private String email;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean active;

    // Tipos de cliente disponibles
    public enum ClientType {
        PERSONAL, BUSINESS
    }

    // Métodos para verificar el tipo de cliente
    public boolean isPersonal() {
        return customerType != null && customerType.contains(ClientType.PERSONAL);
    }

    public boolean isBusiness() {
        return customerType != null && customerType.contains(ClientType.BUSINESS);
    }

    // Métodos específicos de reglas de negocio
    public boolean canHaveSavingsAccount() {
        return isPersonal();
    }

    public boolean canHaveFixedTermAccount() {
        return isPersonal();
    }

    public boolean canHaveMultipleCheckingAccounts() {
        return isBusiness();
    }

    public boolean canHavePersonalCheckingAccount() {
        return isPersonal();
    }

    // Para compatibilidad con el código existente
    public List<ClientType> getTypes() {
        return customerType;
    }
}