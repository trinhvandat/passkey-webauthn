package com.leonard.web_authn.shared.exception;

public class NotFoundException extends BaseException {

    public NotFoundException() {
        super(ErrorCode.NOT_FOUND);
    }

    public NotFoundException(String objectType, String identifier) {
        super(ErrorCode.NOT_FOUND);
        addParam("objectType", objectType);
        addParam("identifier", identifier);
        addMessage("$objectType with identifier $identifier not found.");
    }
}
