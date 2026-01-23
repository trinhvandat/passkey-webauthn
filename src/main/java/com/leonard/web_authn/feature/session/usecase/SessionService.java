package com.leonard.web_authn.feature.session.usecase;

import com.leonard.web_authn.feature.session.adapter.repository.UserSessionRepository;
import com.leonard.web_authn.feature.session.domain.UserSession;
import com.leonard.web_authn.feature.session.domain.exception.InvalidRefreshTokenException;
import com.leonard.web_authn.feature.session.domain.exception.SessionExpiredException;
import com.leonard.web_authn.feature.authorization.usecase.AuthorizationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
public class SessionService {

    private static final int ACCESS_TOKEN_VALIDITY_SECONDS = 900; // 15 minutes
    private static final int REFRESH_TOKEN_VALIDITY_DAYS = 7;

    private final UserSessionRepository userSessionRepository;
    private final AuthorizationService authorizationService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final SecretKey jwtKey;

    public SessionService(
            UserSessionRepository userSessionRepository,
            AuthorizationService authorizationService,
            @Value("${jwt.secret:defaultSecretKeyForDevelopmentOnlyMustBeAtLeast256Bits}") String jwtSecret
    ) {
        this.userSessionRepository = userSessionRepository;
        this.authorizationService = authorizationService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.secureRandom = new SecureRandom();
        this.jwtKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public SessionTokens createSession(String userId, String credentialId, String ipAddress, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = generateRefreshToken();
        String refreshTokenHash = passwordEncoder.encode(refreshToken);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(REFRESH_TOKEN_VALIDITY_DAYS);

        UserSession session = UserSession.builder()
                .id(sessionId)
                .userId(userId)
                .credentialId(credentialId)
                .refreshTokenHash(refreshTokenHash)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceInfo(parseDeviceInfo(userAgent))
                .isActive(true)
                .createdAt(now)
                .lastActivityAt(now)
                .expiresAt(expiresAt)
                .build();

        userSessionRepository.save(session);

        String accessToken = generateAccessToken(userId, sessionId);

        log.info("Session created for user: {}, sessionId: {}", userId, sessionId);

        return new SessionTokens(sessionId, accessToken, refreshToken, ACCESS_TOKEN_VALIDITY_SECONDS);
    }

    @Transactional
    public SessionTokens refreshSession(String refreshToken) {
        // Find all sessions and check refresh token (since it's hashed)
        List<UserSession> activeSessions = userSessionRepository.findAll().stream()
                .filter(s -> s.getIsActive() && s.getRefreshTokenHash() != null)
                .toList();

        for (UserSession session : activeSessions) {
            if (passwordEncoder.matches(refreshToken, session.getRefreshTokenHash())) {
                if (!session.isValid()) {
                    log.warn("Refresh token for expired/revoked session: {}", session.getId());
                    throw new SessionExpiredException();
                }

                // Update last activity
                session.updateActivity();
                userSessionRepository.save(session);

                String accessToken = generateAccessToken(session.getUserId(), session.getId());

                log.info("Session refreshed: {}", session.getId());
                return new SessionTokens(session.getId(), accessToken, null, ACCESS_TOKEN_VALIDITY_SECONDS);
            }
        }

        log.warn("Invalid refresh token attempt");
        throw new InvalidRefreshTokenException();
    }

    public TokenValidationResult validateAccessToken(String accessToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(jwtKey)
                    .build()
                    .parseSignedClaims(accessToken)
                    .getPayload();

            String userId = claims.getSubject();
            String sessionId = claims.get("sessionId", String.class);

            // Verify session is still active
            UserSession session = userSessionRepository.findById(sessionId).orElse(null);
            if (session == null || !session.isValid()) {
                return TokenValidationResult.invalid("Session expired or revoked");
            }

            return TokenValidationResult.valid(userId, sessionId);

        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return TokenValidationResult.invalid("Invalid token");
        }
    }

    public List<SessionInfo> getActiveSessions(String userId) {
        LocalDateTime now = LocalDateTime.now();
        return userSessionRepository.findActiveSessionsByUserId(userId, now).stream()
                .map(this::toSessionInfo)
                .toList();
    }

    @Transactional
    public void revokeSession(String userId, String sessionId) {
        UserSession session = userSessionRepository.findByIdAndUserIdAndIsActiveTrue(sessionId, userId)
                .orElseThrow(SessionExpiredException::new);

        session.revoke("User initiated logout");
        userSessionRepository.save(session);

        log.info("Session revoked: {}", sessionId);
    }

    @Transactional
    public int revokeOtherSessions(String userId, String currentSessionId) {
        LocalDateTime now = LocalDateTime.now();
        int revoked = userSessionRepository.revokeOtherSessions(userId, currentSessionId, now, "User logged out other sessions");
        log.info("Revoked {} other sessions for user: {}", revoked, userId);
        return revoked;
    }

    @Transactional
    public int revokeAllSessions(String userId) {
        LocalDateTime now = LocalDateTime.now();
        int revoked = userSessionRepository.revokeAllUserSessions(userId, now, "All sessions revoked");
        log.info("Revoked all {} sessions for user: {}", revoked, userId);
        return revoked;
    }

    private String generateAccessToken(String userId, String sessionId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + ACCESS_TOKEN_VALIDITY_SECONDS * 1000L);

        AuthorizationService.UserAuthContext authContext = authorizationService.buildAuthContext(userId);

        return Jwts.builder()
                .subject(userId)
                .claim("sessionId", sessionId)
                .claim("roles", new ArrayList<>(authContext.roles()))
                .claim("permissions", new ArrayList<>(authContext.permissions()))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(jwtKey)
                .compact();
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown device";
        }

        // Simple parsing - can be enhanced with a proper library
        if (userAgent.contains("iPhone")) return "iPhone";
        if (userAgent.contains("iPad")) return "iPad";
        if (userAgent.contains("Android")) return "Android device";
        if (userAgent.contains("Windows")) return "Windows PC";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux PC";

        return "Web browser";
    }

    private SessionInfo toSessionInfo(UserSession session) {
        return new SessionInfo(
                session.getId(),
                session.getDeviceInfo(),
                session.getIpAddress(),
                session.getCreatedAt(),
                session.getLastActivityAt()
        );
    }

    public record SessionTokens(String sessionId, String accessToken, String refreshToken, int expiresIn) {}

    public record SessionInfo(
            String id,
            String deviceInfo,
            String ipAddress,
            LocalDateTime createdAt,
            LocalDateTime lastActivityAt
    ) {}

    public record TokenValidationResult(boolean valid, String userId, String sessionId, String error) {
        public static TokenValidationResult valid(String userId, String sessionId) {
            return new TokenValidationResult(true, userId, sessionId, null);
        }

        public static TokenValidationResult invalid(String error) {
            return new TokenValidationResult(false, null, null, error);
        }
    }
}
