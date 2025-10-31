package com.bff.raissaRpa.domain.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class KeyResponse {
    private Integer id;
    private String apiKey;
    private String keyAccess;
    private String secretAccess;
    private Short active;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
