package com.leonard.web_authn.feature.recovery.adapter.web;

import com.leonard.web_authn.feature.recovery.adapter.web.dto.GenerateRecoveryCodesResponseDTO;
import com.leonard.web_authn.feature.recovery.adapter.web.dto.RecoveryCodeStatusDTO;
import com.leonard.web_authn.feature.recovery.adapter.web.dto.UseRecoveryCodeRequestDTO;
import com.leonard.web_authn.feature.recovery.usecase.RecoveryCodeService;
import com.leonard.web_authn.feature.session.usecase.SessionService;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
import com.leonard.web_authn.feature.user.domain.exception.UserNotFoundException;
import com.leonard.web_authn.shared.dto.ApiResponse;
import com.leonard.web_authn.shared.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recovery")
@RequiredArgsConstructor
@Slf4j
public class RecoveryController {

    private final RecoveryCodeService recoveryCodeService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    @PostMapping("/codes/generate")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<GenerateRecoveryCodesResponseDTO> generateRecoveryCodes(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Generating recovery codes for user: {}", user.userId());
        List<String> codes = recoveryCodeService.generateRecoveryCodes(user.userId());
        return ApiResponse.success(new GenerateRecoveryCodesResponseDTO(codes));
    }

    @GetMapping("/codes/status")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RecoveryCodeStatusDTO> getRecoveryCodeStatus(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Getting recovery code status for user: {}", user.userId());
        RecoveryCodeService.RecoveryCodeStatus status = recoveryCodeService.getStatus(user.userId());
        return ApiResponse.success(new RecoveryCodeStatusDTO(
                status.totalCodes(),
                status.usedCodes(),
                status.remainingCodes()
        ));
    }

    @PostMapping("/codes/use")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UseRecoveryCodeResponseDTO> useRecoveryCode(
            @Valid @RequestBody UseRecoveryCodeRequestDTO request,
            HttpServletRequest httpRequest) {
        log.info("Using recovery code for username: {}", request.getUsername());

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Find user by username or email
        String usernameOrEmail = request.getUsername();
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(UserNotFoundException::new);

        // Verify and use the recovery code
        recoveryCodeService.verifyAndUseCode(user.getId(), request.getCode(), ipAddress);

        // Create a session for the user
        SessionService.SessionTokens tokens = sessionService.createSession(
                user.getId(),
                null, // No credential ID for recovery login
                ipAddress,
                userAgent
        );

        log.info("Recovery code used successfully, session created for user: {}", user.getId());

        return ApiResponse.success(new UseRecoveryCodeResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn()
        ));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record UseRecoveryCodeResponseDTO(
            String userId,
            String username,
            String email,
            String displayName,
            String accessToken,
            String refreshToken,
            int expiresIn
    ) {}
}
