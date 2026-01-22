package com.leonard.web_authn.feature.authentication.adapter.web.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StartAuthenticationResponseDTO {
    private String challenge;
    private int timeout;
    private String rpId;
    private String userVerification;
    private List<AllowCredential> allowCredentials;

    @Builder
    @Getter
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class AllowCredential {
        @Builder.Default
        private String type = "public-key";
        private String id;
        private List<String> transports;
    }
}
