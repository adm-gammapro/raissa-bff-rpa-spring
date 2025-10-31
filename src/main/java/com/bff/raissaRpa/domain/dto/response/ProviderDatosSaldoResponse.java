package com.bff.raissaRpa.domain.dto.response;

import lombok.Data;

@Data
public class ProviderDatosSaldoResponse {
    private String id;
    private String name;
    private String number;
    private String branch;
    private String currency;
    private Double balance;
    private Double contable;
}
