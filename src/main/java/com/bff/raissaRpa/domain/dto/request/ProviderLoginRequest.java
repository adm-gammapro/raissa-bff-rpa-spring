package com.bff.raissaRpa.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderLoginRequest {
    private String codigoEmpresa;
    private String codigoUsuario;
    private String claveAcceso;
}
