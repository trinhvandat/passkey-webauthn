package com.leonard.web_authn.feature.security.usecase;

import com.leonard.web_authn.feature.security.adapter.repository.LoginAttemptRepository;
import com.leonard.web_authn.feature.security.domain.LoginAttempt;
import com.leonard.web_authn.feature.security.domain.RateLimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private static final int IP_FAILURES_15MIN_LIMIT = 5;
    private static final int IP_FAILURES_1H_LIMIT = 20;
    private static final int IDENTIFIER_FAILURES_1H_LIMIT = 10;

    private static final int BLOCK_15_MINUTES = 900;
    private static final int BLOCK_1_HOUR = 3600;
    private static final int BLOCK_24_HOURS = 86400;

    private final LoginAttemptRepository loginAttemptRepository;

    public RateLimitResult checkRateLimit(String identifier, String ipAddress) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fifteenMinutesAgo = now.minusMinutes(15);
        LocalDateTime oneHourAgo = now.minusHours(1);

        // Count failures
        int ipFailures15min = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, fifteenMinutesAgo);
        int ipFailures1h = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, oneHourAgo);
        int identifierFailures1h = loginAttemptRepository.countFailedAttemptsByIdentifierSince(identifier, oneHourAgo);

        // Check limits (strictest first)
        if (ipFailures1h >= IP_FAILURES_1H_LIMIT) {
            log.warn("IP {} blocked for 24h - {} failures in 1 hour", ipAddress, ipFailures1h);
            return RateLimitResult.blocked("IP blocked for excessive attempts", BLOCK_24_HOURS);
        }

        if (identifierFailures1h >= IDENTIFIER_FAILURES_1H_LIMIT) {
            log.warn("Identifier {} rate limited - {} failures in 1 hour", identifier, identifierFailures1h);
            return RateLimitResult.blocked("Account locked - too many failed attempts", BLOCK_1_HOUR, true);
        }

        if (ipFailures15min >= IP_FAILURES_15MIN_LIMIT) {
            log.warn("IP {} blocked for 15min - {} failures in 15 minutes", ipAddress, ipFailures15min);
            return RateLimitResult.blocked("Too many attempts - please wait", BLOCK_15_MINUTES);
        }

        return RateLimitResult.allowed();
    }

    @Transactional
    public void recordAttempt(String identifier, String ipAddress, boolean success, String failureReason) {
        LoginAttempt attempt = LoginAttempt.builder()
                .identifier(identifier)
                .ipAddress(ipAddress)
                .success(success)
                .failureReason(failureReason)
                .createdAt(LocalDateTime.now())
                .build();

        loginAttemptRepository.save(attempt);

        if (success) {
            // Clear recent failed attempts for this identifier on successful login
            loginAttemptRepository.clearFailedAttemptsForIdentifier(identifier, LocalDateTime.now().minusHours(1));
        }
    }

    @Transactional
    public void cleanupOldAttempts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = loginAttemptRepository.deleteOldAttempts(cutoff);
        log.info("Cleaned up {} old login attempts", deleted);
    }
}
