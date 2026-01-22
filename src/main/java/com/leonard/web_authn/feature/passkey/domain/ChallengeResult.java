package com.leonard.web_authn.feature.passkey.domain;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Builder
@Data
public class ChallengeResult {
    private String challenge;
    private String rpName;
    private String rpId;
    private String userId;
    private String username;
    private String userDisplayName;
    private List<Integer> pubKeyCreds;
    private int timeout;
    private String authenticatorAttachment;
    private String authenticatorResidentKey;
    private String authenticationUserVerification;
    private String attestation;
    private List<ExcludeCredential> excludeCredentials;


    @Builder
    @Data
    public static class ExcludeCredential {
        @Builder.Default
        private String type = "public-key";
        private String id;
        private List<String> transports;
    }
}
