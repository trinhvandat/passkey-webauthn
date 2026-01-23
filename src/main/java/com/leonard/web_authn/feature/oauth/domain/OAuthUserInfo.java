package com.leonard.web_authn.feature.oauth.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OAuthUserInfo {
    private String providerUserId;
    private String email;
    private String name;
    private String avatarUrl;
    private boolean emailVerified;
}
