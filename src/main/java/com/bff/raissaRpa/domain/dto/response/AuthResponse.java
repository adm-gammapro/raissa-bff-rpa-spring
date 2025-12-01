package com.bff.raissaRpa.domain.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("document_number")
    private String documentNumber;

    private boolean success;
    private String message;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    @JsonProperty("transactionId")
    private String transactionId;

    private String token;

    @JsonProperty("SessionToken")
    private String sessionToken;
}
