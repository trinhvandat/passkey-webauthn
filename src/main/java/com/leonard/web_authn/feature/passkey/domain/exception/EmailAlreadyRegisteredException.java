package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class EmailAlreadyRegisteredException extends BaseException {

    public EmailAlreadyRegisteredException() {
        super(ErrorCode.EMAIL_REGISTERED);
    }
}
