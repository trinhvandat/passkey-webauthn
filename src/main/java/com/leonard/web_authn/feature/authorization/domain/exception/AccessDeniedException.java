package com.leonard.web_authn.feature.authorization.domain.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String permission) {
        super("Access denied. Required permission: " + permission);
    }
}
