package com.leonard.web_authn.feature.authentication.adapter.web.mapping;

import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteAuthenticationRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteAuthenticationResponseDTO;
import com.leonard.web_authn.feature.authentication.usecase.command.CompleteAuthenticationCommand;
import com.leonard.web_authn.feature.passkey.domain.AuthenticationResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompleteAuthenticationMapping {

    public static CompleteAuthenticationCommand toCommand(CompleteAuthenticationRequestDTO request) {
        return CompleteAuthenticationCommand.builder()
                .credentialId(request.getCredentialId())
                .rawId(request.getRawId())
                .clientDataJSON(request.getClientDataJSON())
                .authenticatorData(request.getAuthenticatorData())
                .signature(request.getSignature())
                .userHandle(request.getUserHandle())
                .build();
    }

    public static CompleteAuthenticationResponseDTO toResponse(AuthenticationResult result) {
        if (Objects.isNull(result)) {
            return null;
        }
        return CompleteAuthenticationResponseDTO.builder()
                .userId(result.getUserId())
                .username(result.getUsername())
                .email(result.getEmail())
                .displayName(result.getDisplayName())
                .verified(result.isVerified())
                .build();
    }
}
