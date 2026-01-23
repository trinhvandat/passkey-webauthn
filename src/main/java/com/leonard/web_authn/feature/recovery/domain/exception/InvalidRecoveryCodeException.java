package com.leonard.web_authn.feature.recovery.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class InvalidRecoveryCodeException extends BaseException {

    public InvalidRecoveryCodeException() {
        super(ErrorCode.INVALID_RECOVERY_CODE);
    }
}
