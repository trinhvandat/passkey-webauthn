package com.leonard.web_authn.feature.session.adapter.web;

import com.leonard.web_authn.feature.session.adapter.web.dto.RefreshTokenRequestDTO;
import com.leonard.web_authn.feature.session.adapter.web.dto.SessionInfoDTO;
import com.leonard.web_authn.feature.session.adapter.web.dto.TokenResponseDTO;
import com.leonard.web_authn.feature.session.usecase.SessionService;
import com.leonard.web_authn.shared.dto.ApiResponse;
import com.leonard.web_authn.shared.web.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;
    private final AuthenticatedUserResolver userResolver;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<SessionInfoDTO>> getActiveSessions(HttpServletRequest request) {
        String userId = userResolver.requireUserId(request);
        log.info("Getting active sessions for user: {}", userId);
        List<SessionInfoDTO> sessions = sessionService.getActiveSessions(userId).stream()
                .map(this::toDTO)
                .toList();
        return ApiResponse.success(sessions);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<TokenResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        log.info("Refreshing token");
        SessionService.SessionTokens tokens = sessionService.refreshSession(request.getRefreshToken());
        return ApiResponse.success(new TokenResponseDTO(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.expiresIn()
        ));
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(
            HttpServletRequest request,
            @PathVariable String sessionId) {
        String userId = userResolver.requireUserId(request);
        log.info("Revoking session: {} for user: {}", sessionId, userId);
        sessionService.revokeSession(userId, sessionId);
    }

    @PostMapping("/revoke-others")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RevokeOthersResponse> revokeOtherSessions(
            HttpServletRequest request,
            @RequestHeader("X-Session-Id") String currentSessionId) {
        String userId = userResolver.requireUserId(request);
        log.info("Revoking other sessions for user: {}", userId);
        int count = sessionService.revokeOtherSessions(userId, currentSessionId);
        return ApiResponse.success(new RevokeOthersResponse(count));
    }

    @PostMapping("/revoke-all")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RevokeAllResponse> revokeAllSessions(HttpServletRequest request) {
        String userId = userResolver.requireUserId(request);
        log.info("Revoking all sessions for user: {}", userId);
        int count = sessionService.revokeAllSessions(userId);
        return ApiResponse.success(new RevokeAllResponse(count));
    }

    private SessionInfoDTO toDTO(SessionService.SessionInfo info) {
        return SessionInfoDTO.builder()
                .id(info.id())
                .deviceInfo(info.deviceInfo())
                .ipAddress(info.ipAddress())
                .createdAt(info.createdAt())
                .lastActivityAt(info.lastActivityAt())
                .build();
    }

    public record RevokeOthersResponse(int revokedCount) {}
    public record RevokeAllResponse(int revokedCount) {}
}
