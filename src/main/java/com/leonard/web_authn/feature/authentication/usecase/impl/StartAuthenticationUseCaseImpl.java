package com.leonard.web_authn.feature.authentication.usecase.impl;

import com.leonard.web_authn.feature.authentication.usecase.StartAuthenticationUseCase;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyChallengeRepository;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyCredentialRepository;
import com.leonard.web_authn.feature.passkey.config.WebAuthnProperties;
import com.leonard.web_authn.feature.passkey.domain.AuthenticationChallengeResult;
import com.leonard.web_authn.feature.passkey.domain.OperationType;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
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
import java.util.List;
import java.util.UUID;

import static com.leonard.web_authn.feature.passkey.domain.PasskeyConstants.PASSKEY_CHALLENGE_LENGTH_IN_BYTES;
import static com.leonard.web_authn.feature.passkey.domain.PasskeyConstants.PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS;
import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StartAuthenticationUseCaseImpl implements StartAuthenticationUseCase {

    private final SecureRandom secureRandom;
    private final UserRepository userRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final WebAuthnProperties webAuthnProperties;

    @Override
    public AuthenticationChallengeResult execute(String username) {
        if (isBlank(username)) {
            log.error("Username is required for authentication");
            throw new InvalidRequestException();
        }

        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .or(() -> userRepository.findByEmailAndIsActiveTrue(username))
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UserNotFoundException();
                });

        List<AuthenticationChallengeResult.AllowCredential> allowCredentials = passkeyCredentialRepository
                .findByUserIdAndIsActiveTrue(user.getId())
                .stream()
                .map(credential -> AuthenticationChallengeResult.AllowCredential.builder()
                        .id(Base64.getUrlEncoder().withoutPadding().encodeToString(credential.getCredentialId()))
                        .transports(credential.getTransports())
                        .build())
                .toList();

        String challenge = generateChallenge();
        String sessionId = UUID.randomUUID().toString();

        PasskeyChallenges passkeyChallenge = PasskeyChallenges.builder()
                .challenge(challenge)
                .userId(user.getId())
                .sessionId(sessionId)
                .operationType(OperationType.AUTHENTICATION)
                .expiresAt(LocalDateTime.now().plusSeconds(PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS))
                .isUsed(false)
                .build();

        passkeyChallengeRepository.save(passkeyChallenge);

        log.info("Authentication challenge generated for user: {}", user.getId());

        return AuthenticationChallengeResult.builder()
                .challenge(challenge)
                .timeout(webAuthnProperties.getTimeout())
                .rpId(webAuthnProperties.getRpId())
                .userVerification(webAuthnProperties.getUserVerification())
                .allowCredentials(allowCredentials)
                .build();
    }

    private String generateChallenge() {
        byte[] challengeBytes = new byte[PASSKEY_CHALLENGE_LENGTH_IN_BYTES];
        secureRandom.nextBytes(challengeBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
    }
}
