package com.leonard.web_authn.feature.session.adapter.web;

import com.leonard.web_authn.feature.session.adapter.web.dto.RefreshTokenRequestDTO;
import com.leonard.web_authn.feature.session.adapter.web.dto.SessionInfoDTO;
import com.leonard.web_authn.feature.session.adapter.web.dto.TokenResponseDTO;
import com.leonard.web_authn.feature.session.usecase.SessionService;
import com.leonard.web_authn.shared.dto.ApiResponse;
import com.leonard.web_authn.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<SessionInfoDTO>> getActiveSessions(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Getting active sessions for user: {}", user.userId());
        List<SessionInfoDTO> sessions = sessionService.getActiveSessions(user.userId()).stream()
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
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String sessionId) {
        log.info("Revoking session: {} for user: {}", sessionId, user.userId());
        sessionService.revokeSession(user.userId(), sessionId);
    }

    @PostMapping("/revoke-others")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RevokeOthersResponse> revokeOtherSessions(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Revoking other sessions for user: {}", user.userId());
        int count = sessionService.revokeOtherSessions(user.userId(), user.sessionId());
        return ApiResponse.success(new RevokeOthersResponse(count));
    }

    @PostMapping("/revoke-all")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RevokeAllResponse> revokeAllSessions(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Revoking all sessions for user: {}", user.userId());
        int count = sessionService.revokeAllSessions(user.userId());
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
