package com.leonard.web_authn.feature.oauth.domain.exception;

public class OAuthProviderException extends RuntimeException {
    public OAuthProviderException(String message) {
        super(message);
    }

    public OAuthProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
