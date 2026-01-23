package com.leonard.web_authn.feature.oauth.usecase;

import com.leonard.web_authn.feature.oauth.adapter.provider.OAuthProviderStrategy;
import com.leonard.web_authn.feature.oauth.domain.OAuthProvider;
import com.leonard.web_authn.feature.oauth.domain.exception.OAuthProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class OAuthProviderRegistry {

    private final Map<OAuthProvider, OAuthProviderStrategy> providers;

    public OAuthProviderRegistry(List<OAuthProviderStrategy> strategies) {
        this.providers = strategies.stream()
                .filter(OAuthProviderStrategy::isEnabled)
                .collect(Collectors.toMap(OAuthProviderStrategy::getProvider, s -> s));
        log.info("Registered OAuth providers: {}", providers.keySet());
    }

    public OAuthProviderStrategy getProvider(String providerName) {
        OAuthProvider provider;
        try {
            provider = OAuthProvider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OAuthProviderException("Unknown OAuth provider: " + providerName);
        }

        OAuthProviderStrategy strategy = providers.get(provider);
        if (strategy == null) {
            throw new OAuthProviderException("OAuth provider not enabled: " + providerName);
        }
        return strategy;
    }

    public List<String> getEnabledProviders() {
        return providers.keySet().stream()
                .map(OAuthProvider::name)
                .map(String::toLowerCase)
                .toList();
    }
}
