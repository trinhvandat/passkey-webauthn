package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class SignCountInvalidException extends BaseException {
    public SignCountInvalidException() {
        super(ErrorCode.SIGN_COUNT_INVALID);
    }
}
