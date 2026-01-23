package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class CannotDeleteLastPasskeyException extends BaseException {

    public CannotDeleteLastPasskeyException() {
        super(ErrorCode.CANNOT_DELETE_LAST_PASSKEY);
    }
}
