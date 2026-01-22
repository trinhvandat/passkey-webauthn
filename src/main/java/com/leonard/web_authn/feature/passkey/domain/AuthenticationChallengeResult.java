package com.leonard.web_authn.feature.passkey.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class AuthenticationChallengeResult {
    private String challenge;
    private int timeout;
    private String rpId;
    private String userVerification;
    private List<AllowCredential> allowCredentials;

    @Builder
    @Data
    public static class AllowCredential {
        @Builder.Default
        private String type = "public-key";
        private String id;
        private List<String> transports;
    }
}
