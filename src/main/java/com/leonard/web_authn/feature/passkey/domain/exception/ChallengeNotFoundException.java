package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class ChallengeNotFoundException extends BaseException {
    public ChallengeNotFoundException() {
        super(ErrorCode.CHALLENGE_NOT_FOUND);
    }
}
