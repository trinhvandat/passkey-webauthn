package com.leonard.web_authn.feature.authentication.usecase.impl;

import com.leonard.web_authn.feature.authentication.usecase.CompleteRegisterUserUseCase;
import com.leonard.web_authn.feature.authentication.usecase.command.CompleteRegisterCommand;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyChallengeRepository;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyCredentialRepository;
import com.leonard.web_authn.feature.passkey.config.WebAuthnProperties;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import com.leonard.web_authn.feature.passkey.domain.PasskeyCredentials;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeAlreadyUsedException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeExpiredException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeNotFoundException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialVerificationFailedException;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompleteRegisterUserUseCaseImpl implements CompleteRegisterUserUseCase {

    private final WebAuthnManager webAuthnManager;
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final UserRepository userRepository;
    private final WebAuthnProperties webAuthnProperties;
    private final ObjectConverter objectConverter;

    @Override
    public User execute(CompleteRegisterCommand command) {
        if (!command.isValid()) {
            log.error("Invalid complete register command");
            throw new InvalidRequestException();
        }

        byte[] clientDataJSON = Base64.getUrlDecoder().decode(command.getClientDataJSON());
        byte[] attestationObject = Base64.getUrlDecoder().decode(command.getAttestationObject());

        RegistrationRequest registrationRequest = new RegistrationRequest(
                attestationObject,
                clientDataJSON
        );

        RegistrationData registrationData;
        try {
            registrationData = webAuthnManager.parse(registrationRequest);
        } catch (Exception e) {
            log.error("Failed to parse registration data", e);
            throw new CredentialVerificationFailedException("Failed to parse registration data: " + e.getMessage());
        }

        byte[] challengeBytes = registrationData.getCollectedClientData().getChallenge().getValue();
        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
        PasskeyChallenges passkeyChallenge = validateAndMarkChallengeAsUsed(challengeBase64);

        Origin origin = new Origin(webAuthnProperties.getOrigin());
        String rpId = webAuthnProperties.getRpId();
        DefaultChallenge challenge = new DefaultChallenge(challengeBytes);

        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, null);

        Set<AuthenticatorTransport> transports = null;
        if (Objects.nonNull(command.getTransports()) && !command.getTransports().isEmpty()) {
            transports = command.getTransports().stream()
                    .map(AuthenticatorTransport::create)
                    .collect(Collectors.toSet());
        }

        RegistrationParameters registrationParameters = new RegistrationParameters(
                serverProperty,
                null,
                false,
                true
        );

        try {
            webAuthnManager.verify(registrationData, registrationParameters);
        } catch (Exception e) {
            log.error("Failed to verify registration data", e);
            throw new CredentialVerificationFailedException("Failed to verify registration: " + e.getMessage());
        }

        User user = createUser(command, passkeyChallenge.getUserId());

        saveCredential(registrationData, user.getId(), command.getTransports());

        log.info("User registered successfully: userId={}", user.getId());
        return user;
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

    private User createUser(CompleteRegisterCommand command, String userId) {
        User user = User.builder()
                .id(userId)
                .username(command.getUsername())
                .email(command.getEmail())
                .displayName(command.getDisplayName())
                .isActive(true)
                .isEmailVerified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private void saveCredential(RegistrationData registrationData, String userId, List<String> transports) {
        AttestedCredentialData attestedCredentialData = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .getAttestedCredentialData();

        byte[] credentialId = attestedCredentialData.getCredentialId();
        byte[] publicKey = objectConverter.getCborConverter().writeValueAsBytes(attestedCredentialData.getCOSEKey());
        int algorithm = (int) attestedCredentialData.getCOSEKey().getAlgorithm().getValue();

        byte[] aaguid = attestedCredentialData.getAaguid().getBytes();

        long signCount = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .getSignCount();

        boolean backupEligible = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .isFlagBE();

        boolean backupState = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .isFlagBS();

        boolean userVerified = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .isFlagUV();

        String attestationFormat = registrationData
                .getAttestationObject()
                .getFormat();

        PasskeyCredentials credential = PasskeyCredentials.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .credentialId(credentialId)
                .publicKey(publicKey)
                .algorithm(algorithm)
                .signCount(signCount)
                .aaguid(aaguid)
                .transports(transports)
                .backupEligible(backupEligible)
                .backupState(backupState)
                .userVerified(userVerified)
                .attestationFormat(attestationFormat)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        passkeyCredentialRepository.save(credential);
        log.info("Credential saved: credentialId={}", Base64.getUrlEncoder().encodeToString(credentialId));
    }
}
