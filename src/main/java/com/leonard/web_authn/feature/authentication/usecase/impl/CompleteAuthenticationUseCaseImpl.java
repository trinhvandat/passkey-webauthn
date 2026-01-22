package com.leonard.web_authn.feature.authentication.usecase.impl;

import com.leonard.web_authn.feature.authentication.usecase.CompleteAuthenticationUseCase;
import com.leonard.web_authn.feature.authentication.usecase.command.CompleteAuthenticationCommand;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyChallengeRepository;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyCredentialRepository;
import com.leonard.web_authn.feature.passkey.config.WebAuthnProperties;
import com.leonard.web_authn.feature.passkey.domain.AuthenticationResult;
import com.leonard.web_authn.feature.passkey.domain.OperationType;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import com.leonard.web_authn.feature.passkey.domain.PasskeyCredentials;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeAlreadyUsedException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeExpiredException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeNotFoundException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeOperationTypeMismatchException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialNotFoundException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialUserMismatchException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialVerificationFailedException;
import com.leonard.web_authn.feature.passkey.domain.exception.SignCountInvalidException;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
import com.leonard.web_authn.feature.user.domain.exception.UserInactiveException;
import com.leonard.web_authn.feature.user.domain.exception.UserNotFoundException;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompleteAuthenticationUseCaseImpl implements CompleteAuthenticationUseCase {

    private static final int MAX_BASE64_LENGTH = 65536; // 64KB max for authentication data

    private final WebAuthnManager webAuthnManager;
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final UserRepository userRepository;
    private final WebAuthnProperties webAuthnProperties;
    private final ObjectConverter objectConverter;

    @Override
    public AuthenticationResult execute(CompleteAuthenticationCommand command) {
        validateCommand(command);

        byte[] credentialId = decodeBase64Safely(command.getCredentialId(), "credentialId");
        byte[] clientDataJSON = decodeBase64Safely(command.getClientDataJSON(), "clientDataJSON");
        byte[] authenticatorData = decodeBase64Safely(command.getAuthenticatorData(), "authenticatorData");
        byte[] signature = decodeBase64Safely(command.getSignature(), "signature");

        PasskeyCredentials credential = passkeyCredentialRepository
                .findByCredentialIdAndIsActiveTrue(credentialId)
                .orElseThrow(() -> {
                    log.error("Credential not found for credentialId");
                    return new CredentialNotFoundException();
                });

        // Verify credential belongs to the user specified in userHandle (if provided)
        if (command.getUserHandle() != null && !command.getUserHandle().isEmpty()) {
            byte[] userHandleBytes = decodeBase64Safely(command.getUserHandle(), "userHandle");
            String userIdFromHandle = new String(userHandleBytes);
            if (!credential.getUserId().equals(userIdFromHandle)) {
                log.error("Credential-user mismatch: credential belongs to {}, userHandle is {}",
                        credential.getUserId(), userIdFromHandle);
                throw new CredentialUserMismatchException();
            }
        }

        // Verify user exists and is active
        User user = userRepository.findById(credential.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found for userId: {}", credential.getUserId());
                    return new UserNotFoundException();
                });

        if (!user.getIsActive()) {
            log.error("User account is inactive: userId={}", user.getId());
            throw new UserInactiveException();
        }

        AuthenticationRequest authenticationRequest = new AuthenticationRequest(
                credentialId,
                null,
                authenticatorData,
                clientDataJSON,
                signature
        );

        AuthenticationData authenticationData;
        try {
            authenticationData = webAuthnManager.parse(authenticationRequest);
        } catch (Exception e) {
            log.error("Failed to parse authentication data", e);
            throw new CredentialVerificationFailedException("Failed to parse authentication data: " + e.getMessage());
        }

        byte[] challengeBytes = authenticationData.getCollectedClientData().getChallenge().getValue();
        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        // Use atomic operation to validate and mark challenge as used (prevents race condition)
        PasskeyChallenges passkeyChallenge = validateAndMarkChallengeAsUsedAtomic(challengeBase64);

        // Verify challenge was created for this user (credential-to-challenge binding)
        if (passkeyChallenge.getUserId() != null && !passkeyChallenge.getUserId().equals(credential.getUserId())) {
            log.error("Challenge-user mismatch: challenge for {}, credential belongs to {}",
                    passkeyChallenge.getUserId(), credential.getUserId());
            throw new CredentialUserMismatchException();
        }

        Origin origin = new Origin(webAuthnProperties.getOrigin());
        String rpId = webAuthnProperties.getRpId();
        DefaultChallenge challenge = new DefaultChallenge(challengeBytes);

        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, null);

        Authenticator authenticator = createAuthenticator(credential);

        AuthenticationParameters authenticationParameters = new AuthenticationParameters(
                serverProperty,
                authenticator,
                null,
                true
        );

        try {
            webAuthnManager.verify(authenticationData, authenticationParameters);
        } catch (Exception e) {
            log.error("Failed to verify authentication data", e);
            throw new CredentialVerificationFailedException("Failed to verify authentication: " + e.getMessage());
        }

        // Fixed sign count validation - no bypass when newSignCount is 0
        // Only allow if new count > old count, OR both are 0 (authenticator doesn't support sign count)
        long newSignCount = authenticationData.getAuthenticatorData().getSignCount();
        long oldSignCount = credential.getSignCount();

        if (newSignCount < oldSignCount || (newSignCount == oldSignCount && oldSignCount > 0)) {
            log.error("Sign count anomaly detected (possible cloned authenticator): expected > {}, got {}",
                    oldSignCount, newSignCount);
            throw new SignCountInvalidException();
        }

        credential.setSignCount(newSignCount);
        credential.setLastUsedAt(LocalDateTime.now());
        passkeyCredentialRepository.save(credential);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User authenticated successfully: userId={}", user.getId());

        return AuthenticationResult.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .verified(true)
                .build();
    }

    private void validateCommand(CompleteAuthenticationCommand command) {
        if (!command.isValid()) {
            log.error("Invalid complete authentication command");
            throw new InvalidRequestException();
        }

        // Input length validation
        if (command.getCredentialId() != null && command.getCredentialId().length() > MAX_BASE64_LENGTH) {
            log.error("CredentialId too long");
            throw new InvalidRequestException();
        }
        if (command.getClientDataJSON() != null && command.getClientDataJSON().length() > MAX_BASE64_LENGTH) {
            log.error("ClientDataJSON too long");
            throw new InvalidRequestException();
        }
        if (command.getAuthenticatorData() != null && command.getAuthenticatorData().length() > MAX_BASE64_LENGTH) {
            log.error("AuthenticatorData too long");
            throw new InvalidRequestException();
        }
        if (command.getSignature() != null && command.getSignature().length() > MAX_BASE64_LENGTH) {
            log.error("Signature too long");
            throw new InvalidRequestException();
        }
    }

    private byte[] decodeBase64Safely(String base64, String fieldName) {
        try {
            return Base64.getUrlDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 encoding for {}: {}", fieldName, e.getMessage());
            throw new InvalidRequestException();
        }
    }

    /**
     * Atomic validation and marking of challenge as used.
     * This prevents race conditions where two requests try to use the same challenge.
     */
    private PasskeyChallenges validateAndMarkChallengeAsUsedAtomic(String challengeBase64) {
        LocalDateTime now = LocalDateTime.now();

        // First, try to atomically mark the challenge as used
        int updatedRows = passkeyChallengeRepository.markChallengeAsUsedAtomic(
                challengeBase64,
                now,
                now,
                OperationType.AUTHENTICATION
        );

        if (updatedRows == 0) {
            // Atomic update failed - need to determine the exact reason
            PasskeyChallenges passkeyChallenge = passkeyChallengeRepository
                    .findByChallenge(challengeBase64)
                    .orElseThrow(() -> {
                        log.error("Challenge not found: {}", challengeBase64);
                        return new ChallengeNotFoundException();
                    });

            if (passkeyChallenge.getIsUsed()) {
                log.error("Challenge already used: {}", challengeBase64);
                throw new ChallengeAlreadyUsedException();
            }

            if (passkeyChallenge.getExpiresAt().isBefore(now)) {
                log.error("Challenge expired: {}", challengeBase64);
                throw new ChallengeExpiredException();
            }

            if (passkeyChallenge.getOperationType() != OperationType.AUTHENTICATION) {
                log.error("Challenge operation type mismatch. Expected: AUTHENTICATION, Got: {}",
                        passkeyChallenge.getOperationType());
                throw new ChallengeOperationTypeMismatchException();
            }

            // If we reach here, something unexpected happened
            log.error("Failed to mark challenge as used for unknown reason");
            throw new ChallengeAlreadyUsedException();
        }

        // Atomic update succeeded, now fetch the challenge data we need
        return passkeyChallengeRepository.findByChallenge(challengeBase64)
                .orElseThrow(() -> {
                    log.error("Challenge not found after atomic update: {}", challengeBase64);
                    return new ChallengeNotFoundException();
                });
    }

    private Authenticator createAuthenticator(PasskeyCredentials credential) {
        byte[] aaguidBytes = credential.getAaguid() != null ? credential.getAaguid() : new byte[16];
        AAGUID aaguid = new AAGUID(aaguidBytes);

        COSEKey coseKey = objectConverter.getCborConverter().readValue(credential.getPublicKey(), COSEKey.class);

        AttestedCredentialData attestedCredentialData = new AttestedCredentialData(
                aaguid,
                credential.getCredentialId(),
                coseKey
        );

        return new AuthenticatorImpl(
                attestedCredentialData,
                null,
                credential.getSignCount()
        );
    }
}
