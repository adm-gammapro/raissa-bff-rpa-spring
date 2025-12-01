package com.bff.raissaRpa.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderLoginResponse {
    private boolean success;
    private String message;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("TokenAlterno")
    private String tokenAlterno;

    @JsonProperty("SessionToken")
    private String sessionToken;
}
