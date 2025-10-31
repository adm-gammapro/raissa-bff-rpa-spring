package com.bff.raissaRpa.config;

import com.bff.raissaRpa.domain.repository.ProviderRepository;
import com.bff.raissaRpa.exception.InvalidCredentialsException;
import com.bff.raissaRpa.exception.ProviderNotFoundException;
import com.bff.raissaRpa.exception.RpaAuthenticationException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SessionTokenExtractor {
    private final HttpSession httpSession;

    /**
     * Extrae token y providerReference de la sesión HTTP
     * @param transactionId ID de transacción
     * @param apiKey API key del usuario
     * @return Map con "token" y "providerReference"
     * @throws RpaAuthenticationException si la sesión no está disponible
     * @throws InvalidCredentialsException si los datos no se encuentran
     * @throws ProviderNotFoundException si el provider no existe o está inactivo
     */
    public Map<String, String> extractTokenAndProvider(String transactionId, String apiKey) {
        Map<String, String> result = new HashMap<>();

        String sessionKey = transactionId + "_" + apiKey + "_token";
        String providerKey = transactionId + "_" + apiKey + "_provider";

        // Validar que la sesión HTTP exista
        if (httpSession == null) {
            log.error("Sesión HTTP es nula");
            throw new RpaAuthenticationException("Sesión no disponible");
        }

        // Extraer token y provider de la sesión
        Object tokenObj = httpSession.getAttribute(sessionKey);
        Object providerObj = httpSession.getAttribute(providerKey);

        if (tokenObj == null || providerObj == null) {
            logSessionAttributesForDebugging();
            throw new InvalidCredentialsException("Sesión expirada o no encontrada");
        }

        String token = tokenObj.toString().trim();
        String providerReference = providerObj.toString().trim();

        // Validar que no estén vacíos
        if (token.isEmpty()) {
            log.error("Token vacío encontrado en sesión");
            throw new InvalidCredentialsException("Token inválido");
        }

        if (providerReference.isEmpty()) {
            log.error("Provider vacío encontrado en sesión");
            throw new InvalidCredentialsException("Provider inválido");
        }

        log.info("Extracción exitosa - TransactionId: {}, API Key: {}, Provider: {}",
                transactionId, apiKey, providerReference);

        // Retornar resultados
        result.put("token", token);
        result.put("providerReference", providerReference);

        return result;
    }

    /**
     * Método auxiliar para logging de atributos de sesión (debugging)
     */
    private void logSessionAttributesForDebugging() {
        java.util.Enumeration<String> attributeNames = httpSession.getAttributeNames();
        List<String> availableAttributes = new ArrayList<>();
        while (attributeNames.hasMoreElements()) {
            availableAttributes.add(attributeNames.nextElement());
        }
        log.warn("Atributos disponibles en sesión: {}", availableAttributes);
    }
}
