package com.bff.raissaRpa.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatosSaldosApiRequest {
    @JsonProperty("tokenAlterno")
    private String tokenAlterno;

    @JsonProperty("sessionToken")
    private String sessionToken;

    @JsonProperty("codigoUsuario")
    private String codigoUsuario;

    @JsonProperty("numeroCuenta")
    private String numeroCuenta;
}
