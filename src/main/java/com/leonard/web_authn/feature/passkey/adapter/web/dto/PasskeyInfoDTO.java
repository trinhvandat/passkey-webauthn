package com.leonard.web_authn.feature.passkey.adapter.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PasskeyInfoDTO {
    private String id;
    private String credentialId;
    private String deviceName;
    private String deviceType;
    private String algorithm;
    private Long signCount;
    private Boolean backupEligible;
    private Boolean backupState;
    private List<String> transports;
    private String attestationFormat;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
