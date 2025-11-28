package com.bff.raissaRpa.domain.dto.response;

import lombok.Data;

@Data
public class ProviderDatosMovimientosResponse {
    private String id;
    private Double credit;
    private String date;
    private Double debit;
    private String detail;
    private String reference;
    private String operation;
    private String valueDate;
}
