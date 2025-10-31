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
public class ProviderSaldoResponse {
    private String status;
    private String message;
    private List<ProviderDatosSaldoResponse> accounts;

    public static ProviderSaldoResponse error(String message) {
        return new ProviderSaldoResponse(Constantes.KEY_ERROR_CODE, message, null);
    }

    public static ProviderSaldoResponse wrongCredentials() {
        return new ProviderSaldoResponse(Constantes.KEY_WRONG, null, null);
    }
}
