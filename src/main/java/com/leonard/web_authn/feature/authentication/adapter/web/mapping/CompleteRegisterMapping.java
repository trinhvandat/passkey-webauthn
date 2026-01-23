package com.leonard.web_authn.feature.authentication.adapter.web.mapping;

import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteRegisterRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteRegisterResponseDTO;
import com.leonard.web_authn.feature.authentication.usecase.command.CompleteRegisterCommand;
import com.leonard.web_authn.feature.user.domain.User;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompleteRegisterMapping {

    public static CompleteRegisterCommand toCommand(CompleteRegisterRequestDTO request) {
        return CompleteRegisterCommand.builder()
                .username(request.getUsername())
                .displayName(request.getDisplayName())
                .email(request.getEmail())
                .credentialId(request.getCredentialId())
                .rawId(request.getRawId())
                .clientDataJSON(request.getClientDataJSON())
                .attestationObject(request.getAttestationObject())
                .transports(request.getTransports())
                .build();
    }

    public static CompleteRegisterResponseDTO toResponse(User user) {
        if (Objects.isNull(user)) {
            return null;
        }
        return CompleteRegisterResponseDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .registered(true)
                .build();
    }
}
