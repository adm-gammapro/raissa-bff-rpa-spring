package com.bff.raissaRpa.controller;

import com.bff.raissaRpa.domain.dto.request.DatosSaldoBff;
import com.bff.raissaRpa.domain.dto.request.LoginRequest;
import com.bff.raissaRpa.domain.dto.response.AuthResponse;
import com.bff.raissaRpa.domain.dto.response.LoginResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderLoginResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderMovimientoResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderSaldoResponse;
import com.bff.raissaRpa.exception.ApiKeyValidationException;
import com.bff.raissaRpa.exception.ConnectionException;
import com.bff.raissaRpa.exception.EmptyResponseException;
import com.bff.raissaRpa.exception.InvalidCredentialsException;
import com.bff.raissaRpa.exception.ProviderLoginException;
import com.bff.raissaRpa.exception.ProviderNotFoundException;
import com.bff.raissaRpa.exception.RpaAuthenticationException;
import com.bff.raissaRpa.service.RpaService;
import com.bff.raissaRpa.util.Constantes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bff")
@RequiredArgsConstructor
public class RpaController {
    private final RpaService rpaService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                               @RequestBody LoginRequest loginRequest) {
        try {
            log.info("Iniciando login para provider: {}", loginRequest.getProvider());

            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.error("Missing API key"));
            }

            AuthResponse authResponse = rpaService.authenticateAndLogin(loginRequest, apiKey);

            if (authResponse.isSuccess()) {
                return ResponseEntity.ok(LoginResponse.loggedIn(authResponse.getTransactionId()));
            } else {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.wrongCredentials());
            }
        } catch (InvalidCredentialsException | ProviderLoginException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.wrongCredentials());
        } catch (ProviderNotFoundException | ApiKeyValidationException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.error(e.getMessage()));
        } catch (ConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(LoginResponse.error("Error de conexión con el servidor RPA"));
        } catch (RpaAuthenticationException | EmptyResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.wrongCredentials());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.error("Error No controlado"));
        }
    }

    @PostMapping("/logout/{transactionId}")
    public ResponseEntity<LoginResponse> logout(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                @PathVariable String transactionId) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.error("Missing API key"));
            }

            ProviderLoginResponse logoutResponse = rpaService.logout(transactionId, apiKey);

            if (logoutResponse.isSuccess()) {
                return ResponseEntity.ok(LoginResponse.logout("logged_out"));
            } else {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.wrongCredentials());
            }
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.wrongCredentials());
        } catch (ProviderNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.error("Provider no encontrado"));
        } catch (ApiKeyValidationException e) {
            return ResponseEntity.badRequest()
                    .body(LoginResponse.error("API Key inválida"));
        } catch (ConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(LoginResponse.error("Error de conexión con el servidor RPA"));
        } catch (RpaAuthenticationException | ProviderLoginException | EmptyResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.error("Error interno del servidor"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.error("Error No controlado"));
        }
    }

    @PostMapping("/account/{transactionId}")
    public ResponseEntity<ProviderSaldoResponse> saldos(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                        @PathVariable String transactionId,
                                                        @RequestBody(required = false) DatosSaldoBff datos) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ProviderSaldoResponse.error("Missing API key"));
            }

            String usuario = datos != null ? datos.getCodigoUsuario() : null;
            String cuenta = datos != null ? datos.getNumeroCuenta() : null;

            ProviderSaldoResponse saldosResponse = rpaService.saldos(transactionId, apiKey, usuario, cuenta);

            return ResponseEntity.ok()
                    .body(saldosResponse);
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderSaldoResponse.wrongCredentials());
        } catch (ProviderNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderSaldoResponse.error("Provider no encontrado"));
        } catch (ApiKeyValidationException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderSaldoResponse.error("API Key inválida"));
        } catch (ConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ProviderSaldoResponse.error("Error de conexión con el servidor RPA"));
        } catch (RpaAuthenticationException | ProviderLoginException | EmptyResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProviderSaldoResponse.error("Error interno del servidor"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProviderSaldoResponse.error("Error No controlado"));
        }
    }

    @PostMapping("{account_number}/movement/{transactionId}")
    public ResponseEntity<ProviderMovimientoResponse> movimientos(@RequestHeader(value = Constantes.KEY_API_KEY, required = false) String apiKey,
                                                     @PathVariable(name = "account_number") String accountNumber,
                                                     @PathVariable String transactionId,
                                                     @RequestParam(name = "date_start") String dateStart,
                                                     @RequestParam(name = "date_end") String dateEnd,
                                                     @RequestParam boolean detalle) {
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ProviderMovimientoResponse.error("Missing API key"));
            }

            ProviderMovimientoResponse movementResponse = rpaService.movimientos(transactionId, apiKey, accountNumber, dateStart, dateEnd, detalle);

            return ResponseEntity.ok()
                    .body(movementResponse);
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderMovimientoResponse.wrongCredentials());
        } catch (ProviderNotFoundException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderMovimientoResponse.error("Provider no encontrado"));
        } catch (ApiKeyValidationException e) {
            return ResponseEntity.badRequest()
                    .body(ProviderMovimientoResponse.error("API Key inválida"));
        } catch (ConnectionException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ProviderMovimientoResponse.error("Error de conexión con el servidor RPA"));
        } catch (RpaAuthenticationException | ProviderLoginException | EmptyResponseException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProviderMovimientoResponse.error("Error interno del servidor"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ProviderMovimientoResponse.error("Error No controlado"));
        }
    }

    @GetMapping("/session-info")
    public ResponseEntity<Map<String, String>> getSessionInfo(@RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                                              @RequestParam String transactionId) {
        Map<String, String> sessionInfo = new HashMap<>();
        sessionInfo.put("token", rpaService.getTokenFromSession(transactionId, apiKey));

        return ResponseEntity.ok(sessionInfo);
    }
}
