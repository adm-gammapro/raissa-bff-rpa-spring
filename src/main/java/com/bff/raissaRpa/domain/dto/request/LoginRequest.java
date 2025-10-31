package com.bff.raissaRpa.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginRequest {
    private String username;
    private String password;
    private String provider;
    @JsonProperty("company_code")
    private String companyCode;
}
