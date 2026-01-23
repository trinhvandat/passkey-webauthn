package com.leonard.web_authn.feature.authentication.adapter.web.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CompleteAuthenticationResponseDTO {
    private String userId;
    private String username;
    private String email;
    private String displayName;
    private boolean verified;
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private String sessionId;
}
