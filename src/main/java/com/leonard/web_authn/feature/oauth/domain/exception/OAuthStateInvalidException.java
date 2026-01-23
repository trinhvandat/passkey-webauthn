package com.leonard.web_authn.feature.oauth.domain.exception;

public class OAuthStateInvalidException extends RuntimeException {
    public OAuthStateInvalidException() {
        super("Invalid or expired OAuth state");
    }
}
