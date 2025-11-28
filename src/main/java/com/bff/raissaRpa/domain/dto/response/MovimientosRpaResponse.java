package com.bff.raissaRpa.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovimientosRpaResponse {
    private boolean success;
    private String message;
    private String fechaInicio;
    private String fechaFin;
    private Integer count;

    @JsonProperty("transactionId")
    private String transactionId;

    private List<DatosMovimientosRpaResponse> data;
}
