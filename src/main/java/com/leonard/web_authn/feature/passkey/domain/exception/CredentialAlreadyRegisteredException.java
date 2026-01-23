package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class CredentialAlreadyRegisteredException extends BaseException {

    public CredentialAlreadyRegisteredException() {
        super(ErrorCode.CREDENTIAL_ALREADY_REGISTERED);
    }
}
