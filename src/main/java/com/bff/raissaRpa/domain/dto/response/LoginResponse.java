package com.bff.raissaRpa.domain.dto.response;

import com.bff.raissaRpa.util.Constantes;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    private String status;
    private String key;
    private String message;

    public static LoginResponse loggedIn(String key) {
        return new LoginResponse(Constantes.KEY_LOGGED, key, null);
    }

    public static LoginResponse wrongCredentials() {
        return new LoginResponse(Constantes.KEY_WRONG, null, null);
    }

    public static LoginResponse error(String message) {
        return new LoginResponse(Constantes.KEY_ERROR_CODE, null, message);
    }

    public static LoginResponse logout(String message) {
        return new LoginResponse(Constantes.KEY_STATUS, null, message);
    }

    public static LoginResponse providerNotFound() {
        return new LoginResponse(Constantes.KEY_ERROR_CODE, null, "Provider no encontrado o inactivo");
    }

    public static LoginResponse apiKeyInvalid() {
        return new LoginResponse(Constantes.KEY_ERROR_CODE, null, "API Key inválida o no autorizada");
    }

    public static LoginResponse rpaError(String message) {
        return new LoginResponse(Constantes.KEY_ERROR_CODE, null, "Error en servicio RPA: " + message);
    }
}
