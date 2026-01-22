package com.leonard.web_authn.feature.user.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class UserInactiveException extends BaseException {

    public UserInactiveException() {
        super(ErrorCode.USER_INACTIVE);
    }
}
