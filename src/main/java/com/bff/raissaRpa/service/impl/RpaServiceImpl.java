package com.bff.raissaRpa.service.impl;

import com.bff.raissaRpa.domain.dto.request.LoginRequest;
import com.bff.raissaRpa.domain.dto.request.ProviderLoginRequest;
import com.bff.raissaRpa.domain.dto.response.AuthResponse;
import com.bff.raissaRpa.domain.dto.response.DatosCuentaRpaResponse;
import com.bff.raissaRpa.domain.dto.response.DatosMovimientosRpaResponse;
import com.bff.raissaRpa.domain.dto.response.MovimientosRpaResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderDatosMovimientosResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderDatosSaldoResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderLoginResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderMovimientoResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderSaldoResponse;
import com.bff.raissaRpa.domain.dto.response.SaldosRpaResponse;
import com.bff.raissaRpa.domain.entity.Provider;
import com.bff.raissaRpa.domain.entity.Session;
import com.bff.raissaRpa.domain.repository.ProviderRepository;
import com.bff.raissaRpa.domain.repository.SessionRepository;
import com.bff.raissaRpa.exception.ApiKeyValidationException;
import com.bff.raissaRpa.exception.ConnectionException;
import com.bff.raissaRpa.exception.EmptyResponseException;
import com.bff.raissaRpa.exception.InvalidCredentialsException;
import com.bff.raissaRpa.exception.ProviderLoginException;
import com.bff.raissaRpa.exception.ProviderNotFoundException;
import com.bff.raissaRpa.exception.RpaAuthenticationException;
import com.bff.raissaRpa.service.KeyService;
import com.bff.raissaRpa.service.RpaService;
import com.bff.raissaRpa.util.Constantes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RpaServiceImpl implements RpaService {
    private final ProviderRepository providerRepository;
    private final SessionRepository sessionRepository;
    private final RestTemplate restTemplate;
    private final HttpSession httpSession;
    private final KeyService keyService;


    @Value("${rpa.api.base-url}")
    private String rpaBaseUrl;

    @Override
    public AuthResponse authenticateAndLogin(LoginRequest loginRequest, String apiKey) {
        try {
            validateApiKeyInService(apiKey);

            Provider provider = providerRepository
                    .findByReferenceAndActive(loginRequest.getProvider(), (short) 1)
                    .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + loginRequest.getProvider()));

            log.info("Provider encontrado: {} - Ruta: {}", provider.getName(), provider.getRuta());

            AuthResponse authResponse = authenticate(apiKey);

            if (!authResponse.isSuccess()) {
                throw new InvalidCredentialsException(Constantes.KEY_WRONG);
            }

            Session session = sessionRepository.findByTransactionId(authResponse.getTransactionId()).get();
            session.setProvider(provider.getReference());
            sessionRepository.save(session);

            log.info("Token y transactionId guardados en sesión para API Key: {} y usuario: {}", apiKey, loginRequest.getUsername());

            callProviderLogin(provider.getRuta(), provider.getExtra(), authResponse.getTransactionId(), authResponse.getToken(), loginRequest);

            return authResponse;

        } catch (ProviderNotFoundException | InvalidCredentialsException |
                 ApiKeyValidationException | ConnectionException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en authenticateAndLogin: {}", e.getMessage(), e);
            throw new RpaAuthenticationException("Error interno del servidor", e);
        }
    }

    @Override
    public ProviderSaldoResponse saldos(String transactionId, String apiKey) {
        try {
            validateApiKeyInService(apiKey);

            log.info("Ejecutando logout - TransactionId: {}, API Key: {}", transactionId, apiKey);

            Session session = sessionRepository.findByTransactionId(transactionId).get();

            String token = session.getToken();
            String providerReference = session.getProvider();

            Provider provider = providerRepository
                    .findByReferenceAndActive(providerReference, (short) 1)
                    .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

            SaldosRpaResponse saldos = callSaldos(transactionId, token, provider.getRuta());

            return mapToProviderResponse(saldos);

        } catch (ProviderNotFoundException | InvalidCredentialsException |
                 ApiKeyValidationException | ConnectionException e) {
            log.warn("Error específico en logout: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en logout: {}", e.getMessage(), e);
            throw new RpaAuthenticationException("Error interno del servidor", e);
        }
    }

    @Override
    public ProviderMovimientoResponse movimientos(String transactionId,
                                                  String apiKey,
                                                  String numeroCuenta,
                                                  String fechaInicio,
                                                  String fechaFin,
                                                  boolean detalle) {
        try {
            validateApiKeyInService(apiKey);

            log.info("Ejecutando logout - TransactionId: {}, API Key: {}", transactionId, apiKey);

            Session session = sessionRepository.findByTransactionId(transactionId).get();

            String token = session.getToken();
            String providerReference = session.getProvider();

            Provider provider = providerRepository
                    .findByReferenceAndActive(providerReference, (short) 1)
                    .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

            MovimientosRpaResponse movimientosConsolidados;
            if(provider.getHistorico() == 1) {
                int diferenciaDias = calcularDiferenciaDias(fechaInicio, fechaFin);

                if (diferenciaDias == 0) {
                    log.info("Mismo día ({}-{}), usando solo movimientos con detalle", fechaInicio, fechaFin);
                    movimientosConsolidados = callMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);

                } else {
                    log.info("Diferencia de {} días ({}-{}), combinando históricos + detalle", diferenciaDias, fechaInicio, fechaFin);
                    movimientosConsolidados = combinarMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
                }
            } else {
                movimientosConsolidados = callMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
            }

            return mapToProviderMovementsResponse(movimientosConsolidados);

        } catch (ProviderNotFoundException | InvalidCredentialsException |
                 ApiKeyValidationException | ConnectionException e) {
            log.warn("Error específico en la extraccion de movimientos: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en la extraccion de movimientos: {}", e.getMessage(), e);
            throw new RpaAuthenticationException("Error interno del servidor", e);
        }
    }

    @Override
    public ProviderLoginResponse logout(String transactionId, String apiKey) {
        try {
            validateApiKeyInService(apiKey);

            log.info("Ejecutando logout - TransactionId: {}, API Key: {}", transactionId, apiKey);

            Session session = sessionRepository.findByTransactionId(transactionId).get();

            String token = session.getToken();
            String providerReference = session.getProvider();

            Provider provider = providerRepository
                    .findByReferenceAndActive(providerReference, (short) 1)
                    .orElseThrow(() -> new ProviderNotFoundException("Provider no encontrado o inactivo: " + providerReference));

            return callProviderLogout(transactionId, token, provider.getRuta());

        } catch (ProviderNotFoundException | InvalidCredentialsException |
                 ApiKeyValidationException | ConnectionException e) {
            log.warn("Error específico en logout: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado en logout: {}", e.getMessage(), e);
            throw new RpaAuthenticationException("Error interno del servidor", e);
        }
    }

    private void validateApiKeyInService(String apiKey) {
        try {
            var keyResponse = keyService.getKeyByApiKey(apiKey);
            if (keyResponse.getActive() == 0) {
                throw new ApiKeyValidationException("API Key está inactiva: " + apiKey);
            }
            log.debug("API Key validada en servicio: {}", apiKey);
        } catch (ApiKeyValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiKeyValidationException("Error validando API Key. " + e.getMessage(), e);
        }
    }

    @Override
    public String getTokenFromSession(String transactionId, String apikey) {
        String sessionKey = transactionId + "_" + apikey;

        return (String) httpSession.getAttribute(sessionKey);
    }

    /**
     * Metodo que llama el api del rpa que valida credenciales y genera token
     *
     * @param apiKey llave proporcioanda al cliente que contiene credenciales de logueo
     * @return {@link AuthResponse} datos de autenticacion (token)
     */
    private AuthResponse authenticate(String apiKey) {
        try {
            KeyServiceImpl.KeyAccessData keyData = getKeyAccessData(apiKey);

            String url = rpaBaseUrl + "/api/auth/login";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("key_access", keyData.getKeyAccess());
            body.put("secret_access", keyData.getSecretAccess());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            log.info("Autenticando en: {}", url);
            log.debug("Body de autenticación: key_access={}", keyData.getKeyAccess());

            ResponseEntity<AuthResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    AuthResponse.class
            );

            AuthResponse authResponse = response.getBody();
            if (authResponse == null) {
                throw new EmptyResponseException("Respuesta vacía del servidor de autenticación");
            }

            if (Boolean.FALSE.equals(authResponse.isSuccess())) {
                log.warn("Autenticación fallida en RPA: {} - TransactionId: {}",
                        authResponse.getMessage(), authResponse.getTransactionId());
                throw new InvalidCredentialsException(
                        authResponse.getMessage() != null ?
                                authResponse.getMessage() : "Error de autenticación en RPA"
                );
            }

            if (authResponse.getToken() == null || authResponse.getToken().trim().isEmpty()) {
                throw new RpaAuthenticationException("Token no recibido en la autenticación");
            }

            if (authResponse.getTransactionId() == null || authResponse.getTransactionId().trim().isEmpty()) {
                throw new RpaAuthenticationException("TransactionId no recibido en la autenticación");
            }

            log.info("Autenticación exitosa - TransactionId: {}, Usuario: {}",
                    authResponse.getTransactionId(), authResponse.getFullName());

            return authResponse;

        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} en autenticación: {}", e.getStatusCode(), e.getResponseBodyAsString());
            String errorMessage = extractErrorMessageFromResponse(e.getResponseBodyAsString());
            throw new InvalidCredentialsException(
                    errorMessage != null ? errorMessage : "Error de autenticación: " + e.getStatusCode()
            );

        } catch (HttpServerErrorException e) {
            log.error("Error del servidor RPA ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ConnectionException("Error interno del servidor RPA: " + e.getStatusCode());

        } catch (ResourceAccessException e) {
            log.error("Error de conexión con el servidor RPA: {}", e.getMessage());
            throw new ConnectionException("No se pudo conectar al servidor de autenticación");

        } catch (EmptyResponseException | InvalidCredentialsException | RpaAuthenticationException e) {
            throw e; // Re-lanzar excepciones específicas
        } catch (Exception e) {
            log.error("Error inesperado en autenticación con API Key {}: {}", apiKey, e.getMessage());
            throw new RpaAuthenticationException("Error en autenticación: " + e.getMessage(), e);
        }
    }

    private void callProviderLogin(String providerRoute, Short indicadorExtraDato, String transactionId, String token, LoginRequest loginRequest) {
        String url = String.format("%s/api/%s/login/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        ProviderLoginRequest providerRequestBody = createProviderRequestBody(indicadorExtraDato, loginRequest);
        HttpEntity<ProviderLoginRequest> request = new HttpEntity<>(providerRequestBody, headers);

        log.info("Llamando al login del provider: {} con indicadorExtraDato: {}", url, indicadorExtraDato);
        log.info("Body enviado al provider: {}", providerRequestBody);

        try {
            ResponseEntity<ProviderLoginResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    ProviderLoginResponse.class
            );
            ProviderLoginResponse providerResponse = response.getBody();

            if (providerResponse != null) {
                if (providerResponse.isSuccess()) {
                    log.info("Login del provider exitoso: {} - TransactionId: {}",
                            providerResponse.getMessage(), providerResponse.getTransactionId());
                } else {
                    throw new ProviderLoginException("Error en login del provider: " + providerResponse.getMessage());
                }
            } else {
                throw new EmptyResponseException("Respuesta vacía del provider login");
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider login: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en login del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider login: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar login del provider: " + e.getMessage(), e);
        }
    }

    private SaldosRpaResponse callSaldos(String transactionId, String token, String providerRoute) {
        String url = String.format("%s/api/%s/saldo/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<SaldosRpaResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    SaldosRpaResponse.class
            );
            SaldosRpaResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider saldo");
                throw new EmptyResponseException("Respuesta vacía del provider saldo");
            }

            if (providerResponse.isSuccess()) {
                log.info("Extraccion de saldos del provider exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Extraccion de saldos falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en Extraccion de saldos del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider Extraccion de saldos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en Extraccion de saldos del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider Extraccion de saldos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider Extraccion de saldos: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar Extraccion de saldos del provider: " + e.getMessage(), e);
        }
    }

    private ProviderLoginResponse callProviderLogout(String transactionId, String token, String providerRoute) {
        String url = String.format("%s/api/%s/logout/%s", rpaBaseUrl, providerRoute, transactionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<ProviderLoginResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    ProviderLoginResponse.class
            );
            ProviderLoginResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider logout");
                throw new EmptyResponseException("Respuesta vacía del provider logout");
            }

            if (providerResponse.isSuccess()) {
                log.info("Logout del provider exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Logout falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en logout del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider logout: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en logout del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider logout: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar logout del provider: " + e.getMessage(), e);
        }
    }

    private ProviderLoginRequest createProviderRequestBody(Short indicadorExtraDato, LoginRequest loginRequest) {
        ProviderLoginRequest providerRequest = new ProviderLoginRequest();

        // Validar campos requeridos
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            throw new InvalidCredentialsException("Username es requerido para el login del provider");
        }

        providerRequest.setCodigoUsuario(loginRequest.getUsername());
        providerRequest.setClaveAcceso(loginRequest.getPassword());

        if (indicadorExtraDato != null && indicadorExtraDato == 1) {
            if (loginRequest.getCompanyCode() == null || loginRequest.getCompanyCode().trim().isEmpty()) {
                throw new InvalidCredentialsException("El provider requiere companyCode pero no fue proporcionado");
            }
            providerRequest.setCodigoEmpresa(loginRequest.getCompanyCode());
        }

        return providerRequest;
    }

    private KeyServiceImpl.KeyAccessData getKeyAccessData(String apiKey) {
        try {
            if (keyService instanceof KeyServiceImpl) {
                KeyServiceImpl keyServiceImpl = (KeyServiceImpl) keyService;
                return keyServiceImpl.getKeyAccessData(apiKey);
            } else {
                throw new ApiKeyValidationException("Servicio KeyService no es una instancia de KeyServiceImpl");
            }
        } catch (ApiKeyValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiKeyValidationException("Error al validar API Key: " + e.getMessage(), e);
        }
    }

    private String extractErrorMessageFromResponse(String responseBody) {
        try {
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(responseBody);

                if (jsonNode.has("message")) {
                    return jsonNode.get("message").asText();
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer mensaje de error del cuerpo de respuesta: {}", e.getMessage());
        }
        return null;
    }

    private ProviderSaldoResponse mapToProviderResponse(SaldosRpaResponse saldosRpaResponse) {
        ProviderSaldoResponse providerResponse = new ProviderSaldoResponse();

        if (saldosRpaResponse == null) {
            providerResponse.setStatus("error");
            providerResponse.setMessage("Respuesta nula del servicio");
            return providerResponse;
        }

        if (saldosRpaResponse.isSuccess()) {
            providerResponse.setStatus("success");

            if (saldosRpaResponse.getData() != null && !saldosRpaResponse.getData().isEmpty()) {
                providerResponse.setAccounts(mapAccountsList(saldosRpaResponse.getData()));
            }
        } else {
            providerResponse.setStatus("error");
            providerResponse.setMessage(getSafeMessage(saldosRpaResponse.getMessage(), "Error en la operación"));
        }

        return providerResponse;
    }

    private List<ProviderDatosSaldoResponse> mapAccountsList(List<DatosCuentaRpaResponse> cuentas) {
        List<ProviderDatosSaldoResponse> accounts = new ArrayList<>();

        for (int i = 0; i < cuentas.size(); i++) {
            DatosCuentaRpaResponse cuenta = cuentas.get(i);
            if (cuenta != null) {
                accounts.add(mapCuentaToProviderAccount(cuenta, i + 1));
            }
        }

        return accounts.isEmpty() ? null : accounts;
    }

    private ProviderDatosSaldoResponse mapCuentaToProviderAccount(DatosCuentaRpaResponse cuenta, int id) {
        ProviderDatosSaldoResponse account = new ProviderDatosSaldoResponse();

        account.setId(String.valueOf(id));
        account.setName(getSafeString(cuenta.getTipoCuenta()));
        account.setNumber(getSafeString(cuenta.getNumeroCuenta()));
        account.setBranch("");
        account.setCurrency(parseCurrency(cuenta.getMoneda()));
        account.setBalance(cuenta.getSaldoDisponible());
        account.setContable(cuenta.getSaldoContable());

        return account;
    }

    private String getSafeString(String value) {
        return value != null ? value : "";
    }

    private String getSafeMessage(String message, String defaultMessage) {
        return message != null ? message : defaultMessage;
    }

    private String parseCurrency(String balanceCurrency) {
        if (balanceCurrency == null) return "PEN";

        String upperCurrency = balanceCurrency.toUpperCase();
        if (upperCurrency.contains("SOL") || upperCurrency.contains("PEN")) {
            return "PEN";
        } else if (upperCurrency.contains("DOL") || upperCurrency.contains("USD")) {
            return "USD";
        }
        return "PEN";
    }

    private ProviderMovimientoResponse mapToProviderMovementsResponse(MovimientosRpaResponse movimientosRpaResponse) {
        ProviderMovimientoResponse providerResponse = new ProviderMovimientoResponse();

        if (movimientosRpaResponse == null) {
            providerResponse.setStatus("error");
            providerResponse.setMessage("Respuesta nula del servicio");
            return providerResponse;
        }

        if (movimientosRpaResponse.isSuccess()) {
            providerResponse.setStatus("success");

            if (movimientosRpaResponse.getData() != null && !movimientosRpaResponse.getData().isEmpty()) {
                providerResponse.setMovements(mapMovementsList(movimientosRpaResponse.getData()));
            }
        } else {
            providerResponse.setStatus("error");
            providerResponse.setMessage(getSafeMessage(movimientosRpaResponse.getMessage(), "Error en la operación"));
        }

        return providerResponse;
    }

    private MovimientosRpaResponse callMovimientos(String transactionId,
                                                   String token,
                                                   Provider provider,
                                                   String numeroCuenta,
                                                   String fechaInicio,
                                                   String fechaFin,
                                                   boolean detalle) {
        String baseUrl = String.format("%s/api/%s/transacciones/%s/%s", rpaBaseUrl, provider.getRuta(), numeroCuenta, transactionId);

        UriComponentsBuilder builder;
        if(provider.getDetalle() == 1) {
            builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("fechaInicio", fechaInicio)
                    .queryParam("fechaFin", fechaFin)
                    .queryParam("detalle", detalle);
        } else {
            builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("fechaInicio", fechaInicio)
                    .queryParam("fechaFin", fechaFin);
        }

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<MovimientosRpaResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    MovimientosRpaResponse.class
            );
            MovimientosRpaResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider movimiento");
                throw new EmptyResponseException("Respuesta vacía del provider movimiento");
            }

            if (providerResponse.isSuccess()) {
                log.info("Extraccion de movimientos del provider exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Extraccion de movimientos falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en Extraccion de movimientos del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider Extraccion de movimientos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en Extraccion de movimientos del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider Extraccion de movimientos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider Extraccion de movimientos: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar Extraccion de movimientos del provider: " + e.getMessage(), e);
        }
    }

    private MovimientosRpaResponse callMovimientosHistoricos(String transactionId,
                                                             String token,
                                                             String providerRoute,
                                                             String numeroCuenta,
                                                             String fechaInicio,
                                                             String fechaFin) {
        String baseUrl = String.format("%s/api/%s/transacciones-historicas/%s/%s", rpaBaseUrl, providerRoute, numeroCuenta, transactionId);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("fechaInicio", fechaInicio)
                .queryParam("fechaFin", fechaFin);

        String url = builder.toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<MovimientosRpaResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    MovimientosRpaResponse.class
            );
            MovimientosRpaResponse providerResponse = response.getBody();

            if (providerResponse == null) {
                log.error("Respuesta vacía del provider saldo");
                throw new EmptyResponseException("Respuesta vacía del provider saldo");
            }

            if (providerResponse.isSuccess()) {
                log.info("Extraccion de saldos del provider exitoso: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());

                return providerResponse;

            } else {
                log.warn("Extraccion de saldos falló: {} - TransactionId: {}",
                        providerResponse.getMessage(), providerResponse.getTransactionId());
                throw new ProviderLoginException("Error en Extraccion de saldos del provider: " + providerResponse.getMessage());
            }
        } catch (HttpClientErrorException e) {
            log.error("Error HTTP {} al llamar al provider Extraccion de saldos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error HTTP " + e.getStatusCode() + " en Extraccion de saldos del provider");

        } catch (HttpServerErrorException e) {
            log.error("Error HTTP {} del servidor provider Extraccion de saldos: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ProviderLoginException("Error del servidor provider: " + e.getStatusCode());

        } catch (EmptyResponseException | ProviderLoginException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al llamar al provider Extraccion de saldos: {}", e.getMessage());
            throw new ProviderLoginException("Error al ejecutar Extraccion de saldos del provider: " + e.getMessage(), e);
        }
    }

    private List<ProviderDatosMovimientosResponse> mapMovementsList(List<DatosMovimientosRpaResponse> movimientosRpa) {
        List<ProviderDatosMovimientosResponse> movements = new ArrayList<>();

        for (int i = 0; i < movimientosRpa.size(); i++) {
            DatosMovimientosRpaResponse movimientoRpa = movimientosRpa.get(i);
            if (movimientoRpa != null) {
                movements.add(mapMovimientoToProvider(movimientoRpa, i + 1));
            }
        }

        return movements.isEmpty() ? null : movements;
    }

    private ProviderDatosMovimientosResponse mapMovimientoToProvider(DatosMovimientosRpaResponse movimientoRpa, int consecutiveId) {
        ProviderDatosMovimientosResponse movimiento = new ProviderDatosMovimientosResponse();

        movimiento.setId(String.valueOf(consecutiveId));

        movimiento.setDate(movimientoRpa.getFecha());

        movimiento.setDetail(movimientoRpa.getDescripcion());

        movimiento.setOperation(movimientoRpa.getOperacion());

        movimiento.setValueDate(movimientoRpa.getFechaValor());

        movimiento.setReference(movimientoRpa.getReferencia());

        if ("CREDITO".equalsIgnoreCase(movimientoRpa.getTipo())) {
            movimiento.setCredit(movimientoRpa.getMonto());
            movimiento.setDebit(0.0);
        } else if ("DEBITO".equalsIgnoreCase(movimientoRpa.getTipo())) {
            Double montoPositivo = movimientoRpa.getMonto() != null ? Math.abs(movimientoRpa.getMonto()) : 0.0;
            movimiento.setDebit(montoPositivo);
            movimiento.setCredit(0.0);
        } else {
            movimiento.setCredit(0.0);
            movimiento.setDebit(0.0);
        }

        return movimiento;
    }

    private int calcularDiferenciaDias(String fechaInicio, String fechaFin) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate inicio = LocalDate.parse(fechaInicio, formatter);
            LocalDate fin = LocalDate.parse(fechaFin, formatter);

            long diferencia = ChronoUnit.DAYS.between(inicio, fin);
            return (int) Math.abs(diferencia);

        } catch (Exception e) {
            log.warn("Error calculando diferencia de días, usando valor por defecto: {}", e.getMessage());
            return 0;
        }
    }

    private MovimientosRpaResponse combinarMovimientos(String transactionId,
                                                       String token,
                                                       Provider provider,
                                                       String numeroCuenta,
                                                       String fechaInicio,
                                                       String fechaFin,
                                                       boolean detalle) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate fin = LocalDate.parse(fechaFin, formatter);
            String fechaUltimoDia = fin.format(formatter);

            LocalDate inicioHistoricos = LocalDate.parse(fechaInicio, formatter);
            LocalDate finHistoricos = fin.minusDays(1);
            String fechaInicioHistoricos = inicioHistoricos.format(formatter);
            String fechaFinHistoricos = finHistoricos.format(formatter);

            log.info("Obteniendo históricos: {} a {}", fechaInicioHistoricos, fechaFinHistoricos);
            log.info("Obteniendo detalle último día: {}", fechaUltimoDia);

            MovimientosRpaResponse movimientosHistoricos = null;
            if (!fechaInicioHistoricos.equals(fechaUltimoDia)) {
                movimientosHistoricos = callMovimientosHistoricos(transactionId, token, provider.getRuta(),
                        numeroCuenta, fechaInicioHistoricos, fechaFinHistoricos);
            }

            MovimientosRpaResponse movimientosDetalle = callMovimientos(transactionId, token, provider,
                    numeroCuenta, fechaUltimoDia, fechaUltimoDia, detalle);

            return consolidarMovimientos(movimientosHistoricos, movimientosDetalle, fechaInicio, fechaFin);

        } catch (Exception e) {
            log.error("Error combinando movimientos: {}", e.getMessage());
            // Fallback: usar solo movimientos con detalle
            return callMovimientos(transactionId, token, provider, numeroCuenta, fechaInicio, fechaFin, detalle);
        }
    }

    private MovimientosRpaResponse consolidarMovimientos(MovimientosRpaResponse historicos,
                                                         MovimientosRpaResponse detalle,
                                                         String fechaInicio,
                                                         String fechaFin) {
        MovimientosRpaResponse consolidado = new MovimientosRpaResponse();
        consolidado.setSuccess(true);
        consolidado.setMessage("Movimientos consolidados exitosamente");
        consolidado.setFechaInicio(fechaInicio);
        consolidado.setFechaFin(fechaFin);
        consolidado.setTransactionId(detalle != null ? detalle.getTransactionId() :
                historicos != null ? historicos.getTransactionId() : null);

        List<DatosMovimientosRpaResponse> todosMovimientos = new ArrayList<>();

        if (historicos != null && historicos.isSuccess() &&
                historicos.getData() != null && !historicos.getData().isEmpty()) {
            todosMovimientos.addAll(historicos.getData());
            log.info("Agregados {} movimientos históricos", historicos.getData().size());
        }

        if (detalle != null && detalle.isSuccess() &&
                detalle.getData() != null && !detalle.getData().isEmpty()) {
            todosMovimientos.addAll(detalle.getData());
            log.info("Agregados {} movimientos con detalle", detalle.getData().size());
        }

        consolidado.setData(todosMovimientos);
        consolidado.setCount(todosMovimientos.size());

        log.info("Movimientos consolidados: {} movimientos totales", todosMovimientos.size());

        return consolidado;
    }
}
