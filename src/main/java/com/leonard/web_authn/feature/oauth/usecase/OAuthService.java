package com.leonard.web_authn.feature.oauth.usecase;

import com.leonard.web_authn.feature.authorization.adapter.repository.RoleRepository;
import com.leonard.web_authn.feature.authorization.adapter.repository.UserRoleRepository;
import com.leonard.web_authn.feature.authorization.domain.UserRole;
import com.leonard.web_authn.feature.oauth.adapter.provider.OAuthProviderStrategy;
import com.leonard.web_authn.feature.oauth.adapter.repository.AuthMethodRepository;
import com.leonard.web_authn.feature.oauth.adapter.repository.OAuthStateRepository;
import com.leonard.web_authn.feature.oauth.domain.*;
import com.leonard.web_authn.feature.oauth.domain.exception.AuthMethodAlreadyLinkedException;
import com.leonard.web_authn.feature.oauth.domain.exception.OAuthProviderException;
import com.leonard.web_authn.feature.oauth.domain.exception.OAuthStateInvalidException;
import com.leonard.web_authn.feature.session.usecase.SessionService;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthService {

    private static final int STATE_TTL_MINUTES = 10;

    private final OAuthProviderRegistry providerRegistry;
    private final OAuthStateRepository oAuthStateRepository;
    private final AuthMethodRepository authMethodRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final SessionService sessionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${web-authn.origin:http://localhost:3000}")
    private String frontendOrigin;

    public String startAuthorization(String providerName, String userId) {
        OAuthProviderStrategy provider = providerRegistry.getProvider(providerName);

        String state = generateState();
        String redirectUri = buildCallbackUri(providerName);

        OAuthState oAuthState = OAuthState.builder()
                .state(state)
                .provider(providerName.toUpperCase())
                .userId(userId)
                .redirectUri(redirectUri)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(STATE_TTL_MINUTES))
                .build();
        oAuthStateRepository.save(oAuthState);

        return provider.buildAuthorizationUrl(state, redirectUri);
    }

    @Transactional
    public OAuthResult handleCallback(String providerName, String code, String state) {
        // Validate state (CSRF protection)
        OAuthState oAuthState = oAuthStateRepository.findByStateAndIsUsedFalse(state)
                .orElseThrow(OAuthStateInvalidException::new);

        if (!oAuthState.isValid()) {
            throw new OAuthStateInvalidException();
        }

        oAuthState.setIsUsed(true);
        oAuthStateRepository.save(oAuthState);

        // Exchange code for tokens
        OAuthProviderStrategy provider = providerRegistry.getProvider(providerName);
        String redirectUri = buildCallbackUri(providerName);
        OAuthTokenResponse tokenResponse = provider.exchangeCode(code, redirectUri);

        // Get user info
        OAuthUserInfo userInfo = provider.getUserInfo(tokenResponse.getAccessToken());

        // Check if this OAuth account is already linked to a user
        Optional<AuthMethod> existingAuthMethod = authMethodRepository
                .findByProviderAndProviderUserId(providerName.toUpperCase(), userInfo.getProviderUserId());

        if (existingAuthMethod.isPresent()) {
            // Login: existing user
            AuthMethod authMethod = existingAuthMethod.get();
            authMethod.setLastUsedAt(LocalDateTime.now());
            authMethodRepository.save(authMethod);

            return createSessionForUser(authMethod.getUserId());
        }

        // Check if this is a link operation (userId was set in state)
        if (oAuthState.getUserId() != null) {
            // Link to existing user
            linkAuthMethod(oAuthState.getUserId(), providerName, userInfo);
            return createSessionForUser(oAuthState.getUserId());
        }

        // New user registration via OAuth
        User newUser = createOAuthUser(userInfo);
        linkAuthMethod(newUser.getId(), providerName, userInfo);
        assignDefaultRole(newUser.getId());

        return createSessionForUser(newUser.getId());
    }

    @Transactional
    public void linkProvider(String userId, String providerName, String code, String state) {
        OAuthState oAuthState = oAuthStateRepository.findByStateAndIsUsedFalse(state)
                .orElseThrow(OAuthStateInvalidException::new);

        if (!oAuthState.isValid()) {
            throw new OAuthStateInvalidException();
        }

        oAuthState.setIsUsed(true);
        oAuthStateRepository.save(oAuthState);

        OAuthProviderStrategy provider = providerRegistry.getProvider(providerName);
        String redirectUri = buildCallbackUri(providerName);
        OAuthTokenResponse tokenResponse = provider.exchangeCode(code, redirectUri);
        OAuthUserInfo userInfo = provider.getUserInfo(tokenResponse.getAccessToken());

        // Check if already linked by another user
        Optional<AuthMethod> existing = authMethodRepository
                .findByProviderAndProviderUserId(providerName.toUpperCase(), userInfo.getProviderUserId());
        if (existing.isPresent() && !existing.get().getUserId().equals(userId)) {
            throw new OAuthProviderException("This " + providerName + " account is already linked to another user");
        }

        linkAuthMethod(userId, providerName, userInfo);
    }

    @Transactional
    public void unlinkProvider(String userId, String providerName) {
        AuthMethod authMethod = authMethodRepository.findByUserIdAndProvider(userId, providerName.toUpperCase())
                .orElseThrow(() -> new OAuthProviderException("Auth method not found"));

        // Ensure user has at least one other auth method
        long activeMethodCount = authMethodRepository.findByUserIdAndIsActiveTrue(userId).size();
        if (activeMethodCount <= 1) {
            throw new OAuthProviderException("Cannot unlink the last authentication method");
        }

        authMethod.setIsActive(false);
        authMethodRepository.save(authMethod);

        log.info("Unlinked {} from user {}", providerName, userId);
    }

    private void linkAuthMethod(String userId, String providerName, OAuthUserInfo userInfo) {
        if (authMethodRepository.existsByUserIdAndProvider(userId, providerName.toUpperCase())) {
            throw new AuthMethodAlreadyLinkedException();
        }

        AuthMethod authMethod = AuthMethod.builder()
                .userId(userId)
                .provider(providerName.toUpperCase())
                .providerUserId(userInfo.getProviderUserId())
                .providerEmail(userInfo.getEmail())
                .providerName(userInfo.getName())
                .providerAvatarUrl(userInfo.getAvatarUrl())
                .isPrimary(false)
                .isActive(true)
                .linkedAt(LocalDateTime.now())
                .lastUsedAt(LocalDateTime.now())
                .build();
        authMethodRepository.save(authMethod);

        log.info("Linked {} to user {}", providerName, userId);
    }

    private User createOAuthUser(OAuthUserInfo userInfo) {
        String username = generateUniqueUsername(userInfo.getName(), userInfo.getEmail());

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(username)
                .email(userInfo.getEmail())
                .displayName(userInfo.getName())
                .isActive(true)
                .isEmailVerified(userInfo.isEmailVerified())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private void assignDefaultRole(String userId) {
        roleRepository.findByName("USER").ifPresent(role -> {
            UserRole userRole = UserRole.builder()
                    .userId(userId)
                    .roleId(role.getId())
                    .assignedBy("SYSTEM")
                    .assignedAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(userRole);
        });
    }

    private OAuthResult createSessionForUser(String userId) {
        SessionService.SessionTokens tokens = sessionService.createSession(userId, null, null, "OAuth");
        User user = userRepository.findById(userId).orElseThrow();

        return OAuthResult.builder()
                .userId(userId)
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .expiresIn(tokens.expiresIn())
                .sessionId(tokens.sessionId())
                .build();
    }

    private String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildCallbackUri(String providerName) {
        return frontendOrigin + "/oauth/callback/" + providerName.toLowerCase();
    }

    private String generateUniqueUsername(String name, String email) {
        String base = name != null ? name.replaceAll("\\s+", "").toLowerCase() :
                email != null ? email.split("@")[0] : "user";
        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter;
            counter++;
        }
        return username;
    }

    @lombok.Builder
    @lombok.Getter
    public static class OAuthResult {
        private String userId;
        private String username;
        private String email;
        private String displayName;
        private String accessToken;
        private String refreshToken;
        private int expiresIn;
        private String sessionId;
    }
}
