package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class UsernameAlreadyRegisteredException extends BaseException {

    public UsernameAlreadyRegisteredException() {
        super(ErrorCode.USERNAME_REGISTERED);
    }
}
