package com.bff.raissaRpa.domain.dto.response;

import com.bff.raissaRpa.util.Constantes;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderMovimientoResponse {
    private String status;
    private String message;
    private List<ProviderDatosMovimientosResponse> movements;

    public static ProviderMovimientoResponse error(String message) {
        return new ProviderMovimientoResponse(Constantes.KEY_ERROR_CODE, message, null);
    }

    public static ProviderMovimientoResponse wrongCredentials() {
        return new ProviderMovimientoResponse(Constantes.KEY_WRONG, null, null);
    }
}
