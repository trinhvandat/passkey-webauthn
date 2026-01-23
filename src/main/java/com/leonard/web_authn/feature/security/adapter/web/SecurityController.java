package com.leonard.web_authn.feature.security.adapter.web;

import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyAuthenticationLogRepository;
import com.leonard.web_authn.feature.passkey.domain.PasskeyAuthenticationLog;
import com.leonard.web_authn.feature.recovery.adapter.repository.RecoveryCodeRepository;
import com.leonard.web_authn.feature.recovery.domain.RecoveryCode;
import com.leonard.web_authn.feature.security.adapter.web.dto.AuthLogDTO;
import com.leonard.web_authn.feature.security.adapter.web.dto.AuthLogsResponseDTO;
import com.leonard.web_authn.feature.security.adapter.web.dto.LockAccountRequestDTO;
import com.leonard.web_authn.feature.security.adapter.web.dto.LockAccountResponseDTO;
import com.leonard.web_authn.feature.security.adapter.web.dto.UnlockAccountRequestDTO;
import com.leonard.web_authn.feature.security.adapter.web.dto.UnlockAccountResponseDTO;
import com.leonard.web_authn.feature.session.adapter.repository.UserSessionRepository;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
import com.leonard.web_authn.feature.user.domain.exception.UserNotFoundException;
import com.leonard.web_authn.shared.dto.ApiResponse;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import com.leonard.web_authn.shared.web.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/security")
@Slf4j
public class SecurityController {

    private final UserRepository userRepository;
    private final PasskeyAuthenticationLogRepository authLogRepository;
    private final UserSessionRepository userSessionRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticatedUserResolver userResolver;

    public SecurityController(
            UserRepository userRepository,
            PasskeyAuthenticationLogRepository authLogRepository,
            UserSessionRepository userSessionRepository,
            RecoveryCodeRepository recoveryCodeRepository,
            AuthenticatedUserResolver userResolver) {
        this.userRepository = userRepository;
        this.authLogRepository = authLogRepository;
        this.userSessionRepository = userSessionRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.userResolver = userResolver;
    }

    @PostMapping("/account:lock")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<LockAccountResponseDTO> lockAccount(
            HttpServletRequest httpRequest,
            @Valid @RequestBody(required = false) LockAccountRequestDTO request) {
        String userId = userResolver.requireUserId(httpRequest);
        log.info("Locking account for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new UserNotFoundException();
                });

        if (user.getIsLocked()) {
            log.warn("Account already locked for user: {}", userId);
            return ApiResponse.success(LockAccountResponseDTO.builder()
                    .message("Account is already locked")
                    .lockedAt(user.getLockedAt())
                    .build());
        }

        String reason = request != null && request.getReason() != null
                ? request.getReason()
                : "User-initiated lock";

        user.setIsLocked(true);
        user.setLockedAt(LocalDateTime.now());
        user.setLockReason(reason);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Revoke all sessions
        userSessionRepository.revokeAllSessionsForUser(userId);

        log.info("Account locked for user: {}, reason: {}", userId, reason);

        return ApiResponse.success(LockAccountResponseDTO.builder()
                .message("Account locked successfully")
                .lockedAt(user.getLockedAt())
                .build());
    }

    @PostMapping("/account:unlock")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UnlockAccountResponseDTO> unlockAccount(
            @Valid @RequestBody UnlockAccountRequestDTO request) {
        log.info("Unlocking account for username: {}", request.getUsername());

        String usernameOrEmail = request.getUsername();
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    log.error("User not found: {}", usernameOrEmail);
                    return new UserNotFoundException();
                });

        if (!user.getIsLocked()) {
            log.warn("Account not locked for user: {}", request.getUsername());
            return ApiResponse.success(UnlockAccountResponseDTO.builder()
                    .message("Account is not locked")
                    .build());
        }

        // Verify recovery code
        List<RecoveryCode> codes = recoveryCodeRepository.findByUserIdAndIsUsedFalse(user.getId());
        boolean validCode = false;

        for (RecoveryCode code : codes) {
            if (code.getExpiresAt() != null && code.getExpiresAt().isBefore(LocalDateTime.now())) {
                continue;
            }
            if (passwordEncoder.matches(request.getRecoveryCode(), code.getCodeHash())) {
                code.setIsUsed(true);
                code.setUsedAt(LocalDateTime.now());
                recoveryCodeRepository.save(code);
                validCode = true;
                break;
            }
        }

        if (!validCode) {
            log.warn("Invalid recovery code for unlock attempt: {}", request.getUsername());
            throw new InvalidRequestException();
        }

        user.setIsLocked(false);
        user.setLockedAt(null);
        user.setLockReason(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Account unlocked for user: {}", request.getUsername());

        return ApiResponse.success(UnlockAccountResponseDTO.builder()
                .message("Account unlocked successfully")
                .build());
    }

    @GetMapping("/auth-logs")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthLogsResponseDTO> getAuthLogs(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean success) {
        String userId = userResolver.requireUserId(httpRequest);
        log.info("Getting auth logs for user: {}, page: {}, size: {}, success: {}", userId, page, size, success);

        // Validate page size
        if (size > 100) {
            size = 100;
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<PasskeyAuthenticationLog> logs;
        if (success != null) {
            logs = authLogRepository.findByUserIdAndSuccess(userId, success, pageable);
        } else {
            logs = authLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        List<AuthLogDTO> logDTOs = logs.getContent().stream()
                .map(this::toAuthLogDTO)
                .toList();

        return ApiResponse.success(AuthLogsResponseDTO.builder()
                .logs(logDTOs)
                .total(logs.getTotalElements())
                .page(page)
                .size(size)
                .build());
    }

    private AuthLogDTO toAuthLogDTO(PasskeyAuthenticationLog log) {
        return AuthLogDTO.builder()
                .id(log.getId())
                .operationType(log.getOperationType().name())
                .success(log.getSuccess())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .countryCode(log.getCountryCode())
                .city(log.getCity())
                .errorCode(log.getErrorCode())
                .signCountAnomaly(log.getSignCountAnomaly())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
