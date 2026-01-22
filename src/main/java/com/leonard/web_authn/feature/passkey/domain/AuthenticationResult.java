package com.leonard.web_authn.feature.passkey.domain;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuthenticationResult {
    private String userId;
    private String username;
    private String email;
    private String displayName;
    private boolean verified;
}
