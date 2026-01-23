package com.leonard.web_authn.feature.oauth.domain.exception;

public class AuthMethodAlreadyLinkedException extends RuntimeException {
    public AuthMethodAlreadyLinkedException() {
        super("This authentication method is already linked to your account");
    }
}
