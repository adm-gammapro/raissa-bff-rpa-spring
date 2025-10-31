package com.bff.raissaRpa.domain.dto.response;

import lombok.Data;

@Data
public class DatosCuentaRpaResponse {
    private Double saldoDisponible;
    private String tipoCuenta;
    private String moneda;
    private String numeroCuenta;
    private Double saldoContable;
}
