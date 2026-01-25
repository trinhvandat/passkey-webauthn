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
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GitHubOAuthProvider implements OAuthProviderStrategy {

    private static final String DEFAULT_AUTH_URI = "https://github.com/login/oauth/authorize";
    private static final String DEFAULT_TOKEN_URI = "https://github.com/login/oauth/access_token";
    private static final String DEFAULT_USER_INFO_URI = "https://api.github.com/user";
    private static final String DEFAULT_EMAILS_URI = "https://api.github.com/user/emails";
    private static final String DEFAULT_SCOPES = "user:email,read:user";

    private final OAuthProperties.ProviderConfig config;
    private final RestClient restClient;

    public GitHubOAuthProvider(OAuthProperties oAuthProperties) {
        this.config = oAuthProperties.getProviders().getOrDefault("github", new OAuthProperties.ProviderConfig());
        this.restClient = RestClient.create();
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GITHUB;
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
                "&scope=" + encode(scopes.replace(",", " ")) +
                "&state=" + encode(state);
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
                    .header("Accept", "application/json")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.containsKey("error")) {
                String error = response != null ? (String) response.get("error_description") : "Unknown error";
                throw new OAuthProviderException("GitHub token exchange failed: " + error);
            }

            return OAuthTokenResponse.builder()
                    .accessToken((String) response.get("access_token"))
                    .tokenType((String) response.get("token_type"))
                    .scope((String) response.get("scope"))
                    .build();

        } catch (OAuthProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to exchange code with GitHub", e);
            throw new OAuthProviderException("Failed to exchange authorization code with GitHub", e);
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
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new OAuthProviderException("Failed to get user info from GitHub");
            }

            String email = (String) response.get("email");
            boolean emailVerified = false;

            // GitHub may not return email in the main user endpoint if it's private
            // Need to fetch from /user/emails endpoint
            if (email == null || email.isBlank()) {
                Map<String, Object> emailInfo = fetchPrimaryEmail(accessToken);
                if (emailInfo != null) {
                    email = (String) emailInfo.get("email");
                    emailVerified = Boolean.TRUE.equals(emailInfo.get("verified"));
                }
            } else {
                // If email is returned, fetch verification status from emails endpoint
                Map<String, Object> emailInfo = fetchPrimaryEmail(accessToken);
                if (emailInfo != null) {
                    emailVerified = Boolean.TRUE.equals(emailInfo.get("verified"));
                }
            }

            // GitHub returns ID as integer
            Object idObj = response.get("id");
            String providerUserId = idObj != null ? String.valueOf(idObj) : null;

            return OAuthUserInfo.builder()
                    .providerUserId(providerUserId)
                    .email(email)
                    .name((String) response.get("name"))
                    .avatarUrl((String) response.get("avatar_url"))
                    .emailVerified(emailVerified)
                    .build();

        } catch (OAuthProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get user info from GitHub", e);
            throw new OAuthProviderException("Failed to get user info from GitHub", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchPrimaryEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri(DEFAULT_EMAILS_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(List.class);

            if (emails == null || emails.isEmpty()) {
                return null;
            }

            // Find primary email
            for (Map<String, Object> emailObj : emails) {
                if (Boolean.TRUE.equals(emailObj.get("primary"))) {
                    return emailObj;
                }
            }

            // Fallback to first verified email
            for (Map<String, Object> emailObj : emails) {
                if (Boolean.TRUE.equals(emailObj.get("verified"))) {
                    return emailObj;
                }
            }

            // Fallback to first email
            return emails.get(0);

        } catch (Exception e) {
            log.warn("Failed to fetch emails from GitHub", e);
            return null;
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
