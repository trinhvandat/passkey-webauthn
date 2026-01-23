package com.leonard.web_authn.feature.security.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class AccountLockedException extends BaseException {

    public AccountLockedException() {
        super(ErrorCode.ACCOUNT_LOCKED);
    }

    public AccountLockedException(String reason) {
        super(ErrorCode.ACCOUNT_LOCKED);
        addMessage(reason);
    }
}
