package com.nttdata.banking.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {
    private String id;
    private String name;
    private String documentType;
    private String documentNumber;
    private String email;
    private String phone;
    private String address;

    // Cada cliente tiene un conjunto de tipos (pueden ser varios)
    @Builder.Default
    private List<ClientType> types = new ArrayList<>();

    // Tipos de cliente disponibles
    public enum ClientType {
        PERSONAL, BUSINESS
    }

    // Métodos para verificar el tipo de cliente
    public boolean isPersonal() {
        return types.contains(ClientType.PERSONAL);
    }

    public boolean isBusiness() {
        return types.contains(ClientType.BUSINESS);
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
}