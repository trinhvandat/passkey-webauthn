package com.leonard.web_authn.feature.passkey.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class CredentialVerificationFailedException extends BaseException {
    public CredentialVerificationFailedException() {
        super(ErrorCode.CREDENTIAL_VERIFICATION_FAILED);
    }

    public CredentialVerificationFailedException(String message) {
        super(ErrorCode.CREDENTIAL_VERIFICATION_FAILED);
        addMessage(message);
    }
}
