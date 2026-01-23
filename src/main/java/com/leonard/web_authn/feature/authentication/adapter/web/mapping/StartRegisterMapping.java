package com.leonard.web_authn.feature.authentication.adapter.web.mapping;

import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartRegisterRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartRegisterResponseDTO;
import com.leonard.web_authn.feature.authentication.usecase.command.RegisterUserCommand;
import com.leonard.web_authn.feature.passkey.domain.ChallengeResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StartRegisterMapping {

    public static RegisterUserCommand createRegisterCommand(StartRegisterRequestDTO requestDTO) {
        return RegisterUserCommand.builder()
                .displayName(requestDTO.getDisplayName())
                .username(requestDTO.getUsername())
                .email(requestDTO.getEmail())
                .build();
    }

    public static StartRegisterResponseDTO convertToResponse(ChallengeResult challengeResult) {
        if (Objects.isNull(challengeResult)) {
            return null;
        }


        List<StartRegisterResponseDTO.PubKeyCredParam> pubKeyCredParams = new ArrayList<>();
        if (Objects.nonNull(challengeResult.getPubKeyCreds())) {
            pubKeyCredParams = challengeResult.getPubKeyCreds().stream()
                    .map(pubKeyCred -> StartRegisterResponseDTO.PubKeyCredParam.builder().alg(pubKeyCred).build())
                    .toList();
        }

        List<StartRegisterResponseDTO.ExcludeCredential> excludeCredentials = new ArrayList<>();
        if (Objects.nonNull(challengeResult.getExcludeCredentials())) {
            excludeCredentials = challengeResult.getExcludeCredentials().stream()
                    .map(credential -> StartRegisterResponseDTO.ExcludeCredential.builder()
                            .transports(credential.getTransports())
                            .id(credential.getId())
                            .type(credential.getType())
                            .build())
                    .toList();
        }


        return StartRegisterResponseDTO.builder()
                .challenge(challengeResult.getChallenge())
                .rp(
                        StartRegisterResponseDTO.Rp.builder()
                                .name(challengeResult.getRpName())
                                .id(challengeResult.getRpId())
                                .build()
                )
                .user(
                        StartRegisterResponseDTO.User.builder()
                                .id(challengeResult.getUserId())
                                .name(challengeResult.getUsername())
                                .displayName(challengeResult.getUserDisplayName())
                                .build()
                )
                .pubKeyCredParams(pubKeyCredParams)
                .timeout(challengeResult.getTimeout())
                .authenticatorSelection(
                        StartRegisterResponseDTO.AuthenticatorSelection.builder()
                                .authenticatorAttachment(challengeResult.getAuthenticatorAttachment())
                                .residentKey(challengeResult.getAuthenticatorResidentKey())
                                .userVerification(challengeResult.getAuthenticationUserVerification())
                                .build()
                )
                .attestation(challengeResult.getAttestation())
                .excludeCredentials(excludeCredentials)
                .build();
    }
}
