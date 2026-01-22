package com.leonard.web_authn.feature.authentication.usecase.impl;

import com.leonard.web_authn.feature.authentication.usecase.CompleteAuthenticationUseCase;
import com.leonard.web_authn.feature.authentication.usecase.command.CompleteAuthenticationCommand;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyChallengeRepository;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyCredentialRepository;
import com.leonard.web_authn.feature.passkey.config.WebAuthnProperties;
import com.leonard.web_authn.feature.passkey.domain.AuthenticationResult;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import com.leonard.web_authn.feature.passkey.domain.PasskeyCredentials;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeAlreadyUsedException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeExpiredException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeNotFoundException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialNotFoundException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialVerificationFailedException;
import com.leonard.web_authn.feature.passkey.domain.exception.SignCountInvalidException;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
import com.leonard.web_authn.feature.user.domain.exception.UserNotFoundException;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
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

    private final WebAuthnManager webAuthnManager;
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final UserRepository userRepository;
    private final WebAuthnProperties webAuthnProperties;
    private final ObjectConverter objectConverter;

    @Override
    public AuthenticationResult execute(CompleteAuthenticationCommand command) {
        if (!command.isValid()) {
            log.error("Invalid complete authentication command");
            throw new InvalidRequestException();
        }

        byte[] credentialId = Base64.getUrlDecoder().decode(command.getCredentialId());
        byte[] clientDataJSON = Base64.getUrlDecoder().decode(command.getClientDataJSON());
        byte[] authenticatorData = Base64.getUrlDecoder().decode(command.getAuthenticatorData());
        byte[] signature = Base64.getUrlDecoder().decode(command.getSignature());

        PasskeyCredentials credential = passkeyCredentialRepository
                .findByCredentialIdAndIsActiveTrue(credentialId)
                .orElseThrow(() -> {
                    log.error("Credential not found for credentialId");
                    return new CredentialNotFoundException();
                });

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
        PasskeyChallenges passkeyChallenge = validateAndMarkChallengeAsUsed(challengeBase64);

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

        long newSignCount = authenticationData.getAuthenticatorData().getSignCount();
        if (newSignCount > 0 && newSignCount <= credential.getSignCount()) {
            log.error("Sign count anomaly detected: expected > {}, got {}", credential.getSignCount(), newSignCount);
            throw new SignCountInvalidException();
        }

        credential.setSignCount(newSignCount);
        credential.setLastUsedAt(LocalDateTime.now());
        passkeyCredentialRepository.save(credential);

        User user = userRepository.findById(credential.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found for userId: {}", credential.getUserId());
                    return new UserNotFoundException();
                });

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

    private PasskeyChallenges validateAndMarkChallengeAsUsed(String challengeBase64) {
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

        if (passkeyChallenge.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.error("Challenge expired: {}", challengeBase64);
            throw new ChallengeExpiredException();
        }

        passkeyChallenge.setIsUsed(true);
        passkeyChallenge.setUsedAt(LocalDateTime.now());
        passkeyChallengeRepository.save(passkeyChallenge);

        return passkeyChallenge;
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
