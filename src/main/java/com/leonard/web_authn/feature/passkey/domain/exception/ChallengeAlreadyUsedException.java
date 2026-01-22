package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class ChallengeAlreadyUsedException extends BaseException {
    public ChallengeAlreadyUsedException() {
        super(ErrorCode.CHALLENGE_ALREADY_USED);
    }
}
