package com.bff.raissaRpa.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProviderLoginResponse {
    private boolean success;
    private String message;

    @JsonProperty("transactionId")
    private String transactionId;
}
