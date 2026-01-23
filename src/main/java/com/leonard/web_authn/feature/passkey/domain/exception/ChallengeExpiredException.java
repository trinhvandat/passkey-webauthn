package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class ChallengeExpiredException extends BaseException {
    public ChallengeExpiredException() {
        super(ErrorCode.CHALLENGE_EXPIRED);
    }
}
