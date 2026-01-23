package com.leonard.web_authn.shared.web;

import com.leonard.web_authn.feature.authorization.domain.exception.AccessDeniedException;
import com.leonard.web_authn.feature.authorization.usecase.AuthorizationService;
import com.leonard.web_authn.feature.session.usecase.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticatedUserResolver {

    private final SessionService sessionService;
    private final AuthorizationService authorizationService;

    public String resolveUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                SessionService.TokenValidationResult result = sessionService.validateAccessToken(token);
                if (result.valid()) {
                    return result.userId();
                }
                log.debug("Token validation failed: {}", result.error());
            } catch (Exception e) {
                log.debug("Failed to extract userId from JWT: {}", e.getMessage());
            }
        }
        return null;
    }

    public String requireUserId(HttpServletRequest request) {
        String userId = resolveUserId(request);
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return userId;
    }

    public String requirePermission(HttpServletRequest request, String permission) {
        String userId = requireUserId(request);
        authorizationService.requirePermission(userId, permission);
        return userId;
    }

    public String requireAnyPermission(HttpServletRequest request, String... permissions) {
        String userId = requireUserId(request);
        if (!authorizationService.hasAnyPermission(userId, permissions)) {
            throw new AccessDeniedException(String.join(", ", permissions));
        }
        return userId;
    }

    public String requireRole(HttpServletRequest request, String role) {
        String userId = requireUserId(request);
        if (!authorizationService.hasRole(userId, role)) {
            throw new AccessDeniedException("Role: " + role);
        }
        return userId;
    }
}
