package com.leonard.web_authn.feature.passkey.usecase.impl;

import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyChallengeRepository;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyCredentialRepository;
import com.leonard.web_authn.feature.passkey.config.WebAuthnProperties;
import com.leonard.web_authn.feature.passkey.domain.ChallengeResult;
import com.leonard.web_authn.feature.passkey.domain.OperationType;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import com.leonard.web_authn.feature.passkey.usecase.GenerateChallengeUseCase;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import static com.leonard.web_authn.feature.passkey.domain.PasskeyConstants.PASSKEY_CHALLENGE_LENGTH_IN_BYTES;
import static com.leonard.web_authn.feature.passkey.domain.PasskeyConstants.PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS;
import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GenerateChallengeUseCaseImpl implements GenerateChallengeUseCase {

    private final SecureRandom secureRandom;
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final WebAuthnProperties webAuthnProperties;
    private final PasskeyCredentialRepository passkeyCredentialRepository;

    @Override
    public ChallengeResult execute(String userId, String sessionId) {
        if (isBlank(userId) || isBlank(sessionId)) {
            log.error("Invalid request: userId or sessionId is blank");
            throw new InvalidRequestException();
        }
        final var challenge = generateChallenge();
        var passkeyChallenge = PasskeyChallenges.builder()
                .userId(userId)
                .sessionId(sessionId)
                .challenge(challenge)
                .operationType(OperationType.REGISTRATION)
                .expiresAt(LocalDateTime.now().plusSeconds(PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS))
                .isUsed(false)
                .build();
        passkeyChallenge = passkeyChallengeRepository.save(passkeyChallenge);

        final var excludeCredentials = passkeyCredentialRepository.findByUserIdAndIsActiveTrue(userId)
                .stream().map(passkeyCredential -> ChallengeResult.ExcludeCredential.builder()
                        .id(Base64.getEncoder().encodeToString(passkeyCredential.getCredentialId()))
                        .transports(passkeyCredential.getTransports())
                        .build())
                .toList();

        return ChallengeResult.builder()
                .challenge(passkeyChallenge.getChallenge())
                .rpName(webAuthnProperties.getRpName())
                .rpId(webAuthnProperties.getRpId())
                .userId(passkeyChallenge.getUserId())
                .pubKeyCreds(webAuthnProperties.getSupportedAlgorithms())
                .timeout(webAuthnProperties.getTimeout())
                .authenticatorAttachment(webAuthnProperties.getAuthenticatorAttachment())
                .authenticatorResidentKey(webAuthnProperties.getResidentKey())
                .authenticationUserVerification(webAuthnProperties.getUserVerification())
                .attestation(webAuthnProperties.getAttestation())
                .excludeCredentials(excludeCredentials)
                .build();
    }

    private String generateChallenge() {
        byte[] challengeBytes = new byte[PASSKEY_CHALLENGE_LENGTH_IN_BYTES];

        secureRandom.nextBytes(challengeBytes);

        String challenge = Base64.getUrlEncoder()
                .withoutPadding()  // Remove padding (=)
                .encodeToString(challengeBytes);

        log.debug("Challenge generated: length={} bytes, encoded_length={} chars",
                PASSKEY_CHALLENGE_LENGTH_IN_BYTES, challenge.length());

        return challenge;
    }
}
