package com.bff.raissaRpa.service;

import com.bff.raissaRpa.domain.dto.request.LoginRequest;
import com.bff.raissaRpa.domain.dto.response.AuthResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderLoginResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderMovimientoResponse;
import com.bff.raissaRpa.domain.dto.response.ProviderSaldoResponse;

public interface RpaService {
    AuthResponse authenticateAndLogin(LoginRequest loginRequest, String apiKey);

    ProviderSaldoResponse saldos(String transactionId, String apiKey);

    ProviderMovimientoResponse movimientos(String transactionId,
                                           String apiKey,
                                           String numeroCuenta,
                                           String fechaInicio,
                                           String fechaFin,
                                           boolean detalle);

    ProviderLoginResponse logout(String transactionId, String apiKey);

    String getTokenFromSession(String transactionId, String apikey);
}
