package com.leonard.web_authn.feature.authentication.usecase.impl;

import com.leonard.web_authn.feature.authentication.usecase.CompleteRegisterUserUseCase;
import com.leonard.web_authn.feature.authentication.usecase.command.CompleteRegisterCommand;
import com.leonard.web_authn.feature.authorization.adapter.repository.RoleRepository;
import com.leonard.web_authn.feature.authorization.adapter.repository.UserRoleRepository;
import com.leonard.web_authn.feature.authorization.domain.Role;
import com.leonard.web_authn.feature.authorization.domain.UserRole;
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
import com.leonard.web_authn.feature.passkey.domain.exception.EmailAlreadyRegisteredException;
import com.leonard.web_authn.feature.passkey.domain.exception.UsernameAlreadyRegisteredException;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
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

    private static final int MAX_USERNAME_LENGTH = 255;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_DISPLAY_NAME_LENGTH = 255;
    private static final int MAX_BASE64_LENGTH = 65536; // 64KB max for attestation data

    private final WebAuthnManager webAuthnManager;
    private final PasskeyChallengeRepository passkeyChallengeRepository;
    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final WebAuthnProperties webAuthnProperties;
    private final ObjectConverter objectConverter;

    @Override
    public User execute(CompleteRegisterCommand command) {
        validateCommand(command);

        byte[] clientDataJSON = decodeBase64Safely(command.getClientDataJSON(), "clientDataJSON");
        byte[] attestationObject = decodeBase64Safely(command.getAttestationObject(), "attestationObject");

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

        // Use atomic operation to validate and mark challenge as used (prevents race condition)
        PasskeyChallenges passkeyChallenge = validateAndMarkChallengeAsUsedAtomic(challengeBase64);

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

        // Check for duplicate credential before saving (prevents credential reuse attack)
        AttestedCredentialData attestedCredentialData = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .getAttestedCredentialData();

        byte[] credentialId = attestedCredentialData.getCredentialId();
        if (passkeyCredentialRepository.existsByCredentialId(credentialId)) {
            log.error("Credential already registered: {}", Base64.getUrlEncoder().encodeToString(credentialId));
            throw new CredentialAlreadyRegisteredException();
        }

        // Re-validate user uniqueness (TOCTOU protection - user may have been created between start and complete)
        validateUserUniqueness(command.getUsername(), command.getEmail());

        User user = createUser(command, passkeyChallenge.getUserId());

        saveCredential(registrationData, user.getId(), command.getTransports());

        log.info("User registered successfully: userId={}", user.getId());
        return user;
    }

    private void validateCommand(CompleteRegisterCommand command) {
        if (!command.isValid()) {
            log.error("Invalid complete register command");
            throw new InvalidRequestException();
        }

        // Input length validation to prevent DoS and buffer overflow
        if (command.getUsername() != null && command.getUsername().length() > MAX_USERNAME_LENGTH) {
            log.error("Username too long: {} chars", command.getUsername().length());
            throw new InvalidRequestException();
        }
        if (command.getEmail() != null && command.getEmail().length() > MAX_EMAIL_LENGTH) {
            log.error("Email too long: {} chars", command.getEmail().length());
            throw new InvalidRequestException();
        }
        if (command.getDisplayName() != null && command.getDisplayName().length() > MAX_DISPLAY_NAME_LENGTH) {
            log.error("Display name too long: {} chars", command.getDisplayName().length());
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

    private void validateUserUniqueness(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            log.error("Username already registered: {}", username);
            throw new UsernameAlreadyRegisteredException();
        }
        if (email != null && userRepository.existsByEmail(email)) {
            log.error("Email already registered: {}", email);
            throw new EmailAlreadyRegisteredException();
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
                OperationType.REGISTRATION
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

            if (passkeyChallenge.getOperationType() != OperationType.REGISTRATION) {
                log.error("Challenge operation type mismatch. Expected: REGISTRATION, Got: {}",
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

        User savedUser = userRepository.save(user);
        assignDefaultRole(savedUser.getId());
        return savedUser;
    }

    private void assignDefaultRole(String userId) {
        roleRepository.findByName("USER").ifPresent(role -> {
            UserRole userRole = UserRole.builder()
                    .userId(userId)
                    .roleId(role.getId())
                    .role(role)  // Set the role relationship for immediate access within transaction
                    .assignedBy("SYSTEM")
                    .assignedAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
            log.info("Assigned default USER role to user: {}", userId);
        });
    }

    private void saveCredential(RegistrationData registrationData, String userId, List<String> transports) {
        AttestedCredentialData attestedCredentialData = registrationData
                .getAttestationObject()
                .getAuthenticatorData()
                .getAttestedCredentialData();

        byte[] credentialId = attestedCredentialData.getCredentialId();
        byte[] publicKey = objectConverter.getCborConverter().writeValueAsBytes(attestedCredentialData.getCOSEKey());
        int algorithm = (int) attestedCredentialData.getCOSEKey().getAlgorithm().getValue();

        // Handle AAGUID null safely - some authenticators don't provide AAGUID
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
