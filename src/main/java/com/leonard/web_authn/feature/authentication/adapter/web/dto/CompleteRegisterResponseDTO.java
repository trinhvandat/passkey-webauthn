package com.leonard.web_authn.feature.authentication.adapter.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CompleteRegisterResponseDTO {
    private String userId;
    private String username;
    private String email;
    private String displayName;
    private boolean registered;
}
