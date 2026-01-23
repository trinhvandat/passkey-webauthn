package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class CredentialUserMismatchException extends BaseException {

    public CredentialUserMismatchException() {
        super(ErrorCode.CREDENTIAL_USER_MISMATCH);
    }
}
