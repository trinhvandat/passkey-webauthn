package com.leonard.web_authn.feature.oauth.adapter.provider;

import com.leonard.web_authn.feature.oauth.domain.OAuthProvider;
import com.leonard.web_authn.feature.oauth.domain.OAuthTokenResponse;
import com.leonard.web_authn.feature.oauth.domain.OAuthUserInfo;

public interface OAuthProviderStrategy {
    OAuthProvider getProvider();

    String buildAuthorizationUrl(String state, String redirectUri);

    OAuthTokenResponse exchangeCode(String code, String redirectUri);

    OAuthUserInfo getUserInfo(String accessToken);

    boolean isEnabled();
}
