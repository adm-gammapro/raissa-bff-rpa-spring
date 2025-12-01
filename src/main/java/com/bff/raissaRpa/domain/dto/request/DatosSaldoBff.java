package com.bff.raissaRpa.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatosSaldoBff {
    @JsonProperty("codigoUsuario")
    private String codigoUsuario;

    @JsonProperty("numeroCuenta")
    private String numeroCuenta;
}
