package com.leonard.web_authn.feature.passkey.usecase;

import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyCredentialRepository;
import com.leonard.web_authn.feature.passkey.domain.PasskeyCredentials;
import com.leonard.web_authn.feature.passkey.domain.exception.CannotDeleteLastPasskeyException;
import com.leonard.web_authn.feature.passkey.domain.exception.CredentialNotFoundException;
import com.leonard.web_authn.feature.recovery.adapter.repository.RecoveryCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasskeyManagementService {

    private final PasskeyCredentialRepository passkeyCredentialRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;

    public List<PasskeyInfo> listPasskeys(String userId) {
        List<PasskeyCredentials> credentials = passkeyCredentialRepository.findByUserIdAndIsActiveTrue(userId);

        return credentials.stream()
                .map(this::toPasskeyInfo)
                .toList();
    }

    public PasskeyInfo getPasskey(String userId, String credentialId) {
        PasskeyCredentials credential = passkeyCredentialRepository.findById(credentialId)
                .filter(c -> c.getUserId().equals(userId) && c.getIsActive())
                .orElseThrow(CredentialNotFoundException::new);

        return toPasskeyInfo(credential);
    }

    @Transactional
    public PasskeyInfo renamePasskey(String userId, String credentialId, String newName) {
        PasskeyCredentials credential = passkeyCredentialRepository.findById(credentialId)
                .filter(c -> c.getUserId().equals(userId) && c.getIsActive())
                .orElseThrow(CredentialNotFoundException::new);

        credential.setDeviceName(newName);
        passkeyCredentialRepository.save(credential);

        log.info("Passkey renamed: credentialId={}, newName={}", credentialId, newName);
        return toPasskeyInfo(credential);
    }

    @Transactional
    public void deletePasskey(String userId, String credentialId) {
        PasskeyCredentials credential = passkeyCredentialRepository.findById(credentialId)
                .filter(c -> c.getUserId().equals(userId) && c.getIsActive())
                .orElseThrow(CredentialNotFoundException::new);

        // Check if this is the last passkey
        List<PasskeyCredentials> activeCredentials = passkeyCredentialRepository.findByUserIdAndIsActiveTrue(userId);
        if (activeCredentials.size() <= 1) {
            // Check if user has recovery codes
            boolean hasRecoveryCodes = recoveryCodeRepository.existsByUserIdAndIsUsedFalse(userId);
            if (!hasRecoveryCodes) {
                log.warn("Cannot delete last passkey without recovery codes: userId={}", userId);
                throw new CannotDeleteLastPasskeyException();
            }
        }

        // Soft delete
        credential.setIsActive(false);
        passkeyCredentialRepository.save(credential);

        log.info("Passkey deleted: credentialId={}, userId={}", credentialId, userId);
    }

    @Transactional
    public int revokeAllPasskeys(String userId) {
        List<PasskeyCredentials> credentials = passkeyCredentialRepository.findByUserIdAndIsActiveTrue(userId);

        for (PasskeyCredentials credential : credentials) {
            credential.setIsActive(false);
        }

        passkeyCredentialRepository.saveAll(credentials);
        log.info("All passkeys revoked for user: {}, count: {}", userId, credentials.size());

        return credentials.size();
    }

    public int countActivePasskeys(String userId) {
        return passkeyCredentialRepository.findByUserIdAndIsActiveTrue(userId).size();
    }

    private PasskeyInfo toPasskeyInfo(PasskeyCredentials credential) {
        String algorithmName = switch (credential.getAlgorithm()) {
            case -7 -> "ES256";
            case -257 -> "RS256";
            case -8 -> "EdDSA";
            case -35 -> "ES384";
            case -36 -> "ES512";
            default -> "Unknown";
        };

        return new PasskeyInfo(
                credential.getId(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(credential.getCredentialId()),
                credential.getDeviceName(),
                credential.getDeviceType(),
                algorithmName,
                credential.getSignCount(),
                credential.getBackupEligible(),
                credential.getBackupState(),
                credential.getTransports(),
                credential.getAttestationFormat(),
                credential.getCreatedAt(),
                credential.getLastUsedAt()
        );
    }

    public record PasskeyInfo(
            String id,
            String credentialId,
            String deviceName,
            String deviceType,
            String algorithm,
            Long signCount,
            Boolean backupEligible,
            Boolean backupState,
            List<String> transports,
            String attestationFormat,
            LocalDateTime createdAt,
            LocalDateTime lastUsedAt
    ) {}
}
