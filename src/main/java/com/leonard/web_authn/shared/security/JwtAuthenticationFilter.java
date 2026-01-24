package com.leonard.web_authn.shared.security;

import com.leonard.web_authn.feature.session.usecase.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * JWT Authentication Filter that runs once per request.
 * Extracts and validates JWT token from Authorization header,
 * and populates SecurityContext with authenticated user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                SessionService.TokenValidationResult result = sessionService.validateAccessToken(jwt);

                if (result.valid()) {
                    Set<String> roles = result.roles() != null ? new HashSet<>(result.roles()) : Set.of();
                    Set<String> permissions = result.permissions() != null ? new HashSet<>(result.permissions()) : Set.of();

                    AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                            result.userId(),
                            result.sessionId(),
                            roles,
                            permissions
                    );

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    authenticatedUser,
                                    null,
                                    authenticatedUser.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Authenticated user: {}, session: {}, roles: {}",
                            result.userId(), result.sessionId(), roles);
                } else {
                    log.debug("JWT validation failed: {}", result.error());
                }
            }
        } catch (Exception e) {
            log.debug("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
