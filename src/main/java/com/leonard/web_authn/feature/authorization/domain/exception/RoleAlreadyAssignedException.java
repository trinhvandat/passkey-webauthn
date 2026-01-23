package com.leonard.web_authn.feature.authorization.domain.exception;

public class RoleAlreadyAssignedException extends RuntimeException {
    public RoleAlreadyAssignedException() {
        super("Role is already assigned to this user");
    }
}
