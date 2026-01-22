package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class CredentialNotFoundException extends BaseException {
    public CredentialNotFoundException() {
        super(ErrorCode.CREDENTIAL_NOT_FOUND);
    }
}
