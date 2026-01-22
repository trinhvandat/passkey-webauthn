package com.leonard.web_authn.feature.authentication.adapter.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class StartRegisterResponseDTO {
    private String challenge;
    private Rp rp;
    private User user;
    private List<PubKeyCredParam> pubKeyCredParams;
    private int timeout;
    private AuthenticatorSelection authenticatorSelection;
    private String attestation;
    private List<ExcludeCredential> excludeCredentials;

    @Builder
    @Getter
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ExcludeCredential {
        @Builder.Default
        private String type = "public-key";
        private String id;
        private List<String> transports;
    }

    @Builder
    @Getter
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class AuthenticatorSelection {
        private String authenticatorAttachment;
        private String residentKey;
        private String userVerification;
    }

    @Builder
    @Getter
    public static class PubKeyCredParam {
        @Builder.Default
        private String type = "public-key";
        private int alg;
    }

    @Builder
    @Getter
    public static class User {
        private String id;
        private String name;
        private String displayName;
    }

    @Builder
    @Getter
    public static class Rp {
        private String name;
        private String id;
    }
}
