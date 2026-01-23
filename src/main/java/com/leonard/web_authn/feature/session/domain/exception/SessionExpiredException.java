package com.leonard.web_authn.feature.session.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class SessionExpiredException extends BaseException {

    public SessionExpiredException() {
        super(ErrorCode.SESSION_EXPIRED);
    }
}
