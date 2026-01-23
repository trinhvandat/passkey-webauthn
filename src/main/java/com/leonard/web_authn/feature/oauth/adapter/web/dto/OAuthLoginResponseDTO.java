package com.leonard.web_authn.feature.oauth.adapter.web.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OAuthLoginResponseDTO {
    private String userId;
    private String username;
    private String email;
    private String displayName;
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
    private String sessionId;
}
