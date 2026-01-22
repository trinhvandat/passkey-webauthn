package com.leonard.web_authn.shared.exception;

public class InvalidRequestException extends BaseException {
    public InvalidRequestException() {
        super(ErrorCode.INVALID_REQUEST);
    }
}
