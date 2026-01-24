package com.leonard.web_authn.feature.recovery.usecase;

import com.leonard.web_authn.feature.recovery.adapter.repository.RecoveryCodeRepository;
import com.leonard.web_authn.feature.recovery.domain.RecoveryCode;
import com.leonard.web_authn.feature.recovery.domain.exception.InvalidRecoveryCodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryCodeService {

    private static final int CODE_COUNT = 8;
    private static final int CODE_LENGTH = 8;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Excluded confusing chars: I, O, 0, 1
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public List<String> generateRecoveryCodes(String userId) {
        // Delete existing codes
        recoveryCodeRepository.deleteAllByUserId(userId);

        List<String> plainCodes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < CODE_COUNT; i++) {
            String code = generateCode();
            String formattedCode = formatCode(code);
            plainCodes.add(formattedCode);

            RecoveryCode recoveryCode = RecoveryCode.builder()
                    .userId(userId)
                    .codeHash(passwordEncoder.encode(code))
                    .isUsed(false)
                    .createdAt(now)
                    .build();

            recoveryCodeRepository.save(recoveryCode);
        }

        log.info("Generated {} recovery codes for user: {}", CODE_COUNT, userId);
        return plainCodes;
    }

    @Transactional
    public boolean verifyAndUseCode(String userId, String code, String ipAddress) {
        String normalizedCode = normalizeCode(code);

        List<RecoveryCode> unusedCodes = recoveryCodeRepository.findByUserIdAndIsUsedFalse(userId);

        for (RecoveryCode recoveryCode : unusedCodes) {
            if (passwordEncoder.matches(normalizedCode, recoveryCode.getCodeHash())) {
                // Check expiration
                if (recoveryCode.getExpiresAt() != null &&
                    recoveryCode.getExpiresAt().isBefore(LocalDateTime.now())) {
                    log.warn("Recovery code expired for user: {}", userId);
                    throw new InvalidRecoveryCodeException();
                }

                // Mark as used
                recoveryCode.setIsUsed(true);
                recoveryCode.setUsedAt(LocalDateTime.now());
                recoveryCode.setUsedFromIp(ipAddress);
                recoveryCodeRepository.save(recoveryCode);

                log.info("Recovery code used successfully for user: {}", userId);
                return true;
            }
        }

        log.warn("Invalid recovery code attempt for user: {}", userId);
        throw new InvalidRecoveryCodeException();
    }

    public RecoveryCodeStatus getStatus(String userId) {
        LocalDateTime now = LocalDateTime.now();
        int total = recoveryCodeRepository.countTotalByUserId(userId);
        int unused = recoveryCodeRepository.countUnusedByUserId(userId, now);
        int used = recoveryCodeRepository.countUsedByUserId(userId);

        return new RecoveryCodeStatus(total, used, unused);
    }

    public boolean hasUnusedCodes(String userId) {
        return recoveryCodeRepository.existsByUserIdAndIsUsedFalse(userId);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(CODE_CHARS.length());
            code.append(CODE_CHARS.charAt(index));
        }
        return code.toString();
    }

    private String formatCode(String code) {
        // Format: XXXX-XXXX
        return code.substring(0, 4) + "-" + code.substring(4);
    }

    private String normalizeCode(String code) {
        // Remove dashes and convert to uppercase
        return code.replace("-", "").replace(" ", "").toUpperCase();
    }

    public record RecoveryCodeStatus(int totalCodes, int usedCodes, int remainingCodes) {}
}
