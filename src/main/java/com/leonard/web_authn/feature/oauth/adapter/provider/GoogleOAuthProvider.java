package com.leonard.web_authn.feature.oauth.adapter.provider;

import com.leonard.web_authn.feature.oauth.config.OAuthProperties;
import com.leonard.web_authn.feature.oauth.domain.OAuthProvider;
import com.leonard.web_authn.feature.oauth.domain.OAuthTokenResponse;
import com.leonard.web_authn.feature.oauth.domain.OAuthUserInfo;
import com.leonard.web_authn.feature.oauth.domain.exception.OAuthProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Slf4j
public class GoogleOAuthProvider implements OAuthProviderStrategy {

    private static final String DEFAULT_AUTH_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String DEFAULT_USER_INFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String DEFAULT_SCOPES = "openid,email,profile";

    private final OAuthProperties.ProviderConfig config;
    private final RestClient restClient;

    public GoogleOAuthProvider(OAuthProperties oAuthProperties) {
        this.config = oAuthProperties.getProviders().getOrDefault("google", new OAuthProperties.ProviderConfig());
        this.restClient = RestClient.create();
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled()
                && config.getClientId() != null && !config.getClientId().isBlank()
                && config.getClientSecret() != null && !config.getClientSecret().isBlank();
    }

    @Override
    public String buildAuthorizationUrl(String state, String redirectUri) {
        String authUri = config.getAuthorizationUri() != null ? config.getAuthorizationUri() : DEFAULT_AUTH_URI;
        String scopes = config.getScopes() != null ? config.getScopes() : DEFAULT_SCOPES;

        return authUri + "?" +
                "client_id=" + encode(config.getClientId()) +
                "&redirect_uri=" + encode(redirectUri) +
                "&response_type=code" +
                "&scope=" + encode(scopes.replace(",", " ")) +
                "&state=" + encode(state) +
                "&access_type=offline" +
                "&prompt=consent";
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthTokenResponse exchangeCode(String code, String redirectUri) {
        String tokenUri = config.getTokenUri() != null ? config.getTokenUri() : DEFAULT_TOKEN_URI;

        try {
            String body = "grant_type=authorization_code" +
                    "&code=" + encode(code) +
                    "&redirect_uri=" + encode(redirectUri) +
                    "&client_id=" + encode(config.getClientId()) +
                    "&client_secret=" + encode(config.getClientSecret());

            Map<String, Object> response = restClient.post()
                    .uri(tokenUri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.containsKey("error")) {
                String error = response != null ? (String) response.get("error_description") : "Unknown error";
                throw new OAuthProviderException("Google token exchange failed: " + error);
            }

            return OAuthTokenResponse.builder()
                    .accessToken((String) response.get("access_token"))
                    .refreshToken((String) response.get("refresh_token"))
                    .tokenType((String) response.get("token_type"))
                    .scope((String) response.get("scope"))
                    .expiresIn(response.get("expires_in") != null ? ((Number) response.get("expires_in")).longValue() : null)
                    .build();

        } catch (OAuthProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to exchange code with Google", e);
            throw new OAuthProviderException("Failed to exchange authorization code with Google", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String accessToken) {
        String userInfoUri = config.getUserInfoUri() != null ? config.getUserInfoUri() : DEFAULT_USER_INFO_URI;

        try {
            Map<String, Object> response = restClient.get()
                    .uri(userInfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new OAuthProviderException("Failed to get user info from Google");
            }

            return OAuthUserInfo.builder()
                    .providerUserId((String) response.get("sub"))
                    .email((String) response.get("email"))
                    .name((String) response.get("name"))
                    .avatarUrl((String) response.get("picture"))
                    .emailVerified(Boolean.TRUE.equals(response.get("email_verified")))
                    .build();

        } catch (OAuthProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get user info from Google", e);
            throw new OAuthProviderException("Failed to get user info from Google", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
