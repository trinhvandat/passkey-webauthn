package com.leonard.web_authn.feature.oauth.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OAuthTokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String scope;
    private Long expiresIn;
}
