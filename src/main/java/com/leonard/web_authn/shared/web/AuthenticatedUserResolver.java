package com.leonard.web_authn.shared.web;

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

    /**
     * Resolves the authenticated user ID from the request by extracting from JWT token.
     * This is the secure way to identify the user - never trust client-provided user IDs.
     */
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

    /**
     * Resolves the authenticated user ID, throwing exception if not found.
     */
    public String requireUserId(HttpServletRequest request) {
        String userId = resolveUserId(request);
        if (userId == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return userId;
    }
}
