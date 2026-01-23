package com.leonard.web_authn.feature.passkey.usecase.impl;

import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyChallengeRepository;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyCredentialRepository;
import com.leonard.web_authn.feature.passkey.config.WebAuthnProperties;
import com.leonard.web_authn.feature.passkey.domain.ChallengeResult;
import com.leonard.web_authn.feature.passkey.domain.OperationType;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import com.leonard.web_authn.feature.passkey.usecase.StartAddPasskeyUseCase;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
import com.leonard.web_authn.feature.user.domain.exception.UserNotFoundException;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static com.leonard.web_authn.feature.passkey.domain.PasskeyConstants.PASSKEY_CHALLENGE_LENGTH_IN_BYTES;
import static com.leonard.web_authn.feature.passkey.domain.PasskeyConstants.PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS;
import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StartAddPasskeyUseCaseImpl implements StartAddPasskeyUseCase {

    private final SecureRandom secureRandom;
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final UserRepository userRepository;
    private final WebAuthnProperties webAuthnProperties;

    @Override
    public ChallengeResult execute(String userId, String deviceName) {
        if (isBlank(userId)) {
            log.error("Invalid request: userId is blank");
            throw new InvalidRequestException();
        }

        User user = userRepository.findById(userId)
                .filter(User::getIsActive)
                .orElseThrow(() -> {
                    log.error("User not found or inactive: {}", userId);
                    return new UserNotFoundException();
                });

        String sessionId = UUID.randomUUID().toString();
        String challenge = generateChallenge();

        PasskeyChallenges passkeyChallenge = PasskeyChallenges.builder()
                .userId(userId)
                .sessionId(sessionId)
                .challenge(challenge)
                .operationType(OperationType.ADD_CREDENTIAL)
                .expiresAt(LocalDateTime.now().plusSeconds(PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS))
                .isUsed(false)
                .build();
        passkeyChallenge = passkeyChallengeRepository.save(passkeyChallenge);

        var excludeCredentials = passkeyCredentialRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(credential -> ChallengeResult.ExcludeCredential.builder()
                        .id(Base64.getUrlEncoder().withoutPadding().encodeToString(credential.getCredentialId()))
                        .transports(credential.getTransports())
                        .build())
                .toList();

        log.info("Add passkey challenge generated for user: {}, deviceName: {}", userId, deviceName);

        return ChallengeResult.builder()
                .challenge(passkeyChallenge.getChallenge())
                .rpName(webAuthnProperties.getRpName())
                .rpId(webAuthnProperties.getRpId())
                .userId(user.getId())
                .username(user.getUsername())
                .userDisplayName(user.getDisplayName())
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
        return Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
    }
}
