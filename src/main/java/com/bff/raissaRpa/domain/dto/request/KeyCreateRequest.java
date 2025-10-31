package com.bff.raissaRpa.domain.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class KeyCreateRequest {
    @NotBlank
    @JsonProperty("key_access")
    @Size(max = 100, message = "key_access no debe exceder 100 caracteres")
    private String keyAccess;

    @NotBlank
    @JsonProperty("secret_access")
    @Size(max = 100, message = "secret_access no debe exceder 100 caracteres")
    private String secretAccess;

    @Size(max = 25, message = "createdBy no debe exceder 25 caracteres")
    private String createdBy;
}