package com.leonard.web_authn.feature.security.adapter.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AuthLogDTO {
    private Long id;
    private String operationType;
    private Boolean success;
    private String ipAddress;
    private String userAgent;
    private String countryCode;
    private String city;
    private String errorCode;
    private Boolean signCountAnomaly;
    private LocalDateTime createdAt;
}
