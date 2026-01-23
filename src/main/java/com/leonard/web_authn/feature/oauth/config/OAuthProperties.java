package com.leonard.web_authn.feature.oauth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;
        private String scopes;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String baseUrl;
        private String realm;
    }
}
