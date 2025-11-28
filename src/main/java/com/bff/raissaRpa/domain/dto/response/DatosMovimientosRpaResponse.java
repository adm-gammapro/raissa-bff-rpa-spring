package com.bff.raissaRpa.domain.dto.response;

import lombok.Data;

@Data
public class DatosMovimientosRpaResponse {
    private Double monto;
    private String descripcion;
    private String tipo;
    private String fecha;
    private String fechaValor;
    private String operacion;
    private String saldo;
    private String referencia;
}
