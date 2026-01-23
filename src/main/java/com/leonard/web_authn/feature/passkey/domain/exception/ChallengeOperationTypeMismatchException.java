package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class ChallengeOperationTypeMismatchException extends BaseException {

    public ChallengeOperationTypeMismatchException() {
        super(ErrorCode.CHALLENGE_OPERATION_TYPE_MISMATCH);
    }
}
