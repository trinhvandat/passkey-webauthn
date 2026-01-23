package com.leonard.web_authn.feature.passkey.adapter.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompleteAddPasskeyResponseDTO {
    private String id;
    private String credentialId;
    private String deviceName;
    private String deviceType;
    private String algorithm;
}
