package com.leonard.web_authn.feature.passkey.usecase.impl;

import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyChallengeRepository;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyCredentialRepository;
import com.leonard.web_authn.feature.passkey.config.WebAuthnProperties;
import com.leonard.web_authn.feature.passkey.domain.OperationType;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import com.leonard.web_authn.feature.passkey.domain.PasskeyCredentials;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeAlreadyUsedException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeExpiredException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeNotFoundException;
import com.leonard.web_authn.feature.passkey.domain.exception.ChallengeOperationTypeMismatchException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialAlreadyRegisteredException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialVerificationFailedException;
import com.leonard.web_authn.feature.passkey.usecase.CompleteAddPasskeyUseCase;
import com.leonard.web_authn.feature.passkey.usecase.command.CompleteAddPasskeyCommand;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.exception.UserNotFoundException;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticatorTransport;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AAGUID;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompleteAddPasskeyUseCaseImpl implements CompleteAddPasskeyUseCase {

    private static final int MAX_BASE64_LENGTH = 65536;
    private static final int MAX_DEVICE_NAME_LENGTH = 255;

    private final WebAuthnManager webAuthnManager;
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final UserRepository userRepository;
    private final WebAuthnProperties webAuthnProperties;
    private final ObjectConverter objectConverter;

    @Override
    public AddPasskeyResult execute(CompleteAddPasskeyCommand command) {
        validateCommand(command);

        // Verify user exists and is active
        var user = userRepository.findById(command.getUserId())
                .filter(u -> u.getIsActive())
                .orElseThrow(() -> {
                    log.error("User not found or inactive: {}", command.getUserId());
                    return new UserNotFoundException();
                });

        byte[] clientDataJSON = decodeBase64Safely(command.getClientDataJSON(), "clientDataJSON");
        byte[] attestationObject = decodeBase64Safely(command.getAttestationObject(), "attestationObject");

        RegistrationRequest registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON);

        RegistrationData registrationData;
        try {
            registrationData = webAuthnManager.parse(registrationRequest);
        } catch (Exception e) {
            log.error("Failed to parse registration data for add passkey", e);
            throw new CredentialVerificationFailedException("Failed to parse registration data: " + e.getMessage());
        }

        byte[] challengeBytes = registrationData.getCollectedClientData().getChallenge().getValue();
        String challengeBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);

        PasskeyChallenges passkeyChallenge = validateAndMarkChallengeAsUsedAtomic(challengeBase64, command.getUserId());

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
            log.error("Failed to verify add passkey registration data", e);
            throw new CredentialVerificationFailedException("Failed to verify registration: " + e.getMessage());
        }

        AttestedCredentialData attestedCredentialData = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .getAttestedCredentialData();

        byte[] credentialId = attestedCredentialData.getCredentialId();
        if (passkeyCredentialRepository.existsByCredentialId(credentialId)) {
            log.error("Credential already registered: {}", Base64.getUrlEncoder().encodeToString(credentialId));
            throw new CredentialAlreadyRegisteredException();
        }

        PasskeyCredentials credential = saveCredential(registrationData, command.getUserId(), command.getDeviceName(), command.getTransports());

        log.info("New passkey added for user: {}, credentialId: {}", command.getUserId(),
                Base64.getUrlEncoder().encodeToString(credentialId));

        String algorithmName = getAlgorithmName(credential.getAlgorithm());

        return new AddPasskeyResult(
                credential.getId(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(credential.getCredentialId()),
                credential.getDeviceName(),
                credential.getDeviceType(),
                algorithmName
        );
    }

    private void validateCommand(CompleteAddPasskeyCommand command) {
        if (!command.isValid()) {
            log.error("Invalid complete add passkey command");
            throw new InvalidRequestException();
        }

        if (command.getDeviceName() != null && command.getDeviceName().length() > MAX_DEVICE_NAME_LENGTH) {
            log.error("Device name too long: {} chars", command.getDeviceName().length());
            throw new InvalidRequestException();
        }
        if (command.getClientDataJSON() != null && command.getClientDataJSON().length() > MAX_BASE64_LENGTH) {
            log.error("ClientDataJSON too long: {} chars", command.getClientDataJSON().length());
            throw new InvalidRequestException();
        }
        if (command.getAttestationObject() != null && command.getAttestationObject().length() > MAX_BASE64_LENGTH) {
            log.error("AttestationObject too long: {} chars", command.getAttestationObject().length());
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

    private PasskeyChallenges validateAndMarkChallengeAsUsedAtomic(String challengeBase64, String userId) {
        LocalDateTime now = LocalDateTime.now();

        int updatedRows = passkeyChallengeRepository.markChallengeAsUsedAtomic(
                challengeBase64,
                now,
                now,
                OperationType.ADD_CREDENTIAL
        );

        if (updatedRows == 0) {
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

            if (passkeyChallenge.getOperationType() != OperationType.ADD_CREDENTIAL) {
                log.error("Challenge operation type mismatch. Expected: ADD_CREDENTIAL, Got: {}",
                        passkeyChallenge.getOperationType());
                throw new ChallengeOperationTypeMismatchException();
            }

            log.error("Failed to mark challenge as used for unknown reason");
            throw new ChallengeAlreadyUsedException();
        }

        PasskeyChallenges passkeyChallenge = passkeyChallengeRepository.findByChallenge(challengeBase64)
                .orElseThrow(() -> {
                    log.error("Challenge not found after atomic update: {}", challengeBase64);
                    return new ChallengeNotFoundException();
                });

        // Verify the challenge belongs to the correct user
        if (!passkeyChallenge.getUserId().equals(userId)) {
            log.error("Challenge user mismatch. Expected: {}, Got: {}", userId, passkeyChallenge.getUserId());
            throw new ChallengeNotFoundException();
        }

        return passkeyChallenge;
    }

    private PasskeyCredentials saveCredential(RegistrationData registrationData, String userId, String deviceName, java.util.List<String> transports) {
        AttestedCredentialData attestedCredentialData = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .getAttestedCredentialData();

        byte[] credentialId = attestedCredentialData.getCredentialId();
        byte[] publicKey = objectConverter.getCborConverter().writeValueAsBytes(attestedCredentialData.getCOSEKey());
        int algorithm = (int) attestedCredentialData.getCOSEKey().getAlgorithm().getValue();

        AAGUID aaguidObj = attestedCredentialData.getAaguid();
        byte[] aaguid = (aaguidObj != null) ? aaguidObj.getBytes() : null;

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

        // Determine device type from transports
        String deviceType = determineDeviceType(transports);

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
                .deviceName(deviceName)
                .deviceType(deviceType)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return passkeyCredentialRepository.save(credential);
    }

    private String determineDeviceType(java.util.List<String> transports) {
        if (transports == null || transports.isEmpty()) {
            return "Unknown";
        }
        if (transports.contains("internal")) {
            return "Platform";
        }
        if (transports.contains("usb")) {
            return "Security Key (USB)";
        }
        if (transports.contains("nfc")) {
            return "Security Key (NFC)";
        }
        if (transports.contains("ble")) {
            return "Security Key (BLE)";
        }
        if (transports.contains("hybrid")) {
            return "Cross-device";
        }
        return "Unknown";
    }

    private String getAlgorithmName(int algorithm) {
        return switch (algorithm) {
            case -7 -> "ES256";
            case -257 -> "RS256";
            case -8 -> "EdDSA";
            case -35 -> "ES384";
            case -36 -> "ES512";
            default -> "Unknown";
        };
    }
}
