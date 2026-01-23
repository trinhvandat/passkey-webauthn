package com.leonard.web_authn.feature.authentication.adapter.web;

import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteAuthenticationRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteAuthenticationResponseDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteRegisterRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteRegisterResponseDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartAuthenticationRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartAuthenticationResponseDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartRegisterRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartRegisterResponseDTO;
import com.leonard.web_authn.feature.authentication.usecase.CompleteAuthenticationUseCase;
import com.leonard.web_authn.feature.authentication.usecase.CompleteRegisterUserUseCase;
import com.leonard.web_authn.feature.authentication.usecase.StartAuthenticationUseCase;
import com.leonard.web_authn.feature.authentication.usecase.StartRegisterUserUseCase;
import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyAuthenticationLogRepository;
import com.leonard.web_authn.feature.passkey.domain.AuthenticationResult;
import com.leonard.web_authn.feature.passkey.domain.OperationType;
import com.leonard.web_authn.feature.passkey.domain.PasskeyAuthenticationLog;
import com.leonard.web_authn.feature.security.domain.RateLimitResult;
import com.leonard.web_authn.feature.security.domain.exception.AccountLockedException;
import com.leonard.web_authn.feature.security.domain.exception.RateLimitExceededException;
import com.leonard.web_authn.feature.security.usecase.RateLimitService;
import com.leonard.web_authn.feature.session.usecase.SessionService;
import com.leonard.web_authn.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.StartRegisterMapping.convertToResponse;
import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.StartRegisterMapping.createRegisterCommand;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final StartRegisterUserUseCase startRegisterUserUseCase;
    private final CompleteRegisterUserUseCase completeRegisterUserUseCase;
    private final StartAuthenticationUseCase startAuthenticationUseCase;
    private final CompleteAuthenticationUseCase completeAuthenticationUseCase;
    private final RateLimitService rateLimitService;
    private final SessionService sessionService;
    private final PasskeyAuthenticationLogRepository authLogRepository;

    @PostMapping("/register:start")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StartRegisterResponseDTO> startRegister(
            @Valid @RequestBody StartRegisterRequestDTO request) {
        log.info("Starting registration for user: {}", request.getUsername());
        final var command = createRegisterCommand(request);
        final var result = startRegisterUserUseCase.execute(command);
        return ApiResponse.success(convertToResponse(result));
    }

    @PostMapping("/register:complete")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompleteRegisterResponseDTO> completeRegister(
            @Valid @RequestBody CompleteRegisterRequestDTO request) {
        log.info("Completing registration for user: {}", request.getUsername());
        final var command = com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteRegisterMapping.toCommand(request);
        final var result = completeRegisterUserUseCase.execute(command);
        return ApiResponse.success(com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteRegisterMapping.toResponse(result));
    }

    @PostMapping("/authenticate:start")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StartAuthenticationResponseDTO> startAuthentication(
            @Valid @RequestBody StartAuthenticationRequestDTO request,
            HttpServletRequest httpRequest) {
        log.info("Starting authentication for user: {}", request.getUsername());

        String ipAddress = getClientIpAddress(httpRequest);

        // Check rate limiting before processing
        RateLimitResult rateLimitResult = rateLimitService.checkRateLimit(request.getUsername(), ipAddress);
        if (rateLimitResult.isBlocked()) {
            log.warn("Rate limit exceeded for user: {}, ip: {}", request.getUsername(), ipAddress);
            if (rateLimitResult.isShouldLockAccount()) {
                throw new AccountLockedException(rateLimitResult.getReason());
            }
            throw new RateLimitExceededException(rateLimitResult.getReason(), rateLimitResult.getRetryAfterSeconds());
        }

        final var result = startAuthenticationUseCase.execute(request.getUsername());
        return ApiResponse.success(com.leonard.web_authn.feature.authentication.adapter.web.mapping.StartAuthenticationMapping.toResponse(result));
    }

    @PostMapping("/authenticate:complete")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CompleteAuthenticationResponseDTO> completeAuthentication(
            @Valid @RequestBody CompleteAuthenticationRequestDTO request,
            HttpServletRequest httpRequest) {
        log.info("Completing authentication");

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // Extract username from userHandle if available for rate limiting
        String identifier = request.getUserHandle() != null ? request.getUserHandle() : request.getCredentialId();

        try {
            final var command = com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteAuthenticationMapping.toCommand(request);
            final AuthenticationResult result = completeAuthenticationUseCase.execute(command);

            // Record successful attempt
            rateLimitService.recordAttempt(result.getUsername(), ipAddress, true, null);

            // Log successful authentication
            saveAuthLog(result.getUserId(), result.getCredentialId(), OperationType.AUTHENTICATION,
                    true, ipAddress, userAgent, null, null);

            // Create session and generate tokens
            SessionService.SessionTokens tokens = sessionService.createSession(
                    result.getUserId(),
                    result.getCredentialId(),
                    ipAddress,
                    userAgent
            );

            log.info("Authentication successful, session created for user: {}", result.getUserId());

            return ApiResponse.success(CompleteAuthenticationResponseDTO.builder()
                    .userId(result.getUserId())
                    .username(result.getUsername())
                    .email(result.getEmail())
                    .displayName(result.getDisplayName())
                    .verified(result.isVerified())
                    .accessToken(tokens.accessToken())
                    .refreshToken(tokens.refreshToken())
                    .expiresIn(tokens.expiresIn())
                    .sessionId(tokens.sessionId())
                    .build());
        } catch (Exception e) {
            // Record failed attempt
            rateLimitService.recordAttempt(identifier, ipAddress, false, e.getMessage());

            // Log failed authentication
            saveAuthLog(null, null, OperationType.AUTHENTICATION,
                    false, ipAddress, userAgent, e.getClass().getSimpleName(), e.getMessage());

            throw e;
        }
    }

    private void saveAuthLog(String userId, String credentialId, OperationType operationType,
                            boolean success, String ipAddress, String userAgent,
                            String errorCode, String errorMessage) {
        try {
            PasskeyAuthenticationLog authLog = PasskeyAuthenticationLog.builder()
                    .userId(userId != null ? userId : "unknown")
                    .operationType(operationType)
                    .success(success)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .errorCode(errorCode)
                    .errorMessage(errorMessage)
                    .build();
            authLogRepository.save(authLog);
        } catch (Exception e) {
            log.warn("Failed to save authentication log: {}", e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
