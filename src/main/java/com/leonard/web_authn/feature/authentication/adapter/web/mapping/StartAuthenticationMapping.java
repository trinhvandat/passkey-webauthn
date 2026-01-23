package com.leonard.web_authn.feature.authentication.adapter.web.mapping;

import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartAuthenticationResponseDTO;
import com.leonard.web_authn.feature.passkey.domain.AuthenticationChallengeResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StartAuthenticationMapping {

    public static StartAuthenticationResponseDTO toResponse(AuthenticationChallengeResult result) {
        if (Objects.isNull(result)) {
            return null;
        }

        List<StartAuthenticationResponseDTO.AllowCredential> allowCredentials = new ArrayList<>();
        if (Objects.nonNull(result.getAllowCredentials())) {
            allowCredentials = result.getAllowCredentials().stream()
                    .map(cred -> StartAuthenticationResponseDTO.AllowCredential.builder()
                            .id(cred.getId())
                            .type(cred.getType())
                            .transports(cred.getTransports())
                            .build())
                    .toList();
        }

        return StartAuthenticationResponseDTO.builder()
                .challenge(result.getChallenge())
                .timeout(result.getTimeout())
                .rpId(result.getRpId())
                .userVerification(result.getUserVerification())
                .allowCredentials(allowCredentials)
                .build();
    }
}
