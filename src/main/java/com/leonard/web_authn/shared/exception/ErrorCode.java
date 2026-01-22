package com.leonard.web_authn.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    NOT_FOUND("ERR_000001", HttpStatus.NOT_FOUND),
    INVALID_REQUEST("ERR_000002", HttpStatus.BAD_REQUEST),
    EMAIL_REGISTERED("ERR_000003", HttpStatus.CONFLICT),
    CHALLENGE_NOT_FOUND("ERR_000004", HttpStatus.NOT_FOUND),
    CHALLENGE_EXPIRED("ERR_000005", HttpStatus.BAD_REQUEST),
    CHALLENGE_ALREADY_USED("ERR_000006", HttpStatus.BAD_REQUEST),
    CREDENTIAL_VERIFICATION_FAILED("ERR_000007", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("ERR_000008", HttpStatus.NOT_FOUND),
    CREDENTIAL_NOT_FOUND("ERR_000009", HttpStatus.NOT_FOUND),
    SIGN_COUNT_INVALID("ERR_000010", HttpStatus.BAD_REQUEST),
    USERNAME_REGISTERED("ERR_000011", HttpStatus.CONFLICT),
    CREDENTIAL_ALREADY_REGISTERED("ERR_000012", HttpStatus.CONFLICT),
    CHALLENGE_OPERATION_TYPE_MISMATCH("ERR_000013", HttpStatus.BAD_REQUEST),
    USER_INACTIVE("ERR_000014", HttpStatus.FORBIDDEN),
    CREDENTIAL_USER_MISMATCH("ERR_000015", HttpStatus.BAD_REQUEST);

    ErrorCode(String code, HttpStatus statusCode) {
        this.code = code;
        this.statusCode = statusCode;
    }

    private final String code;
    private final HttpStatus statusCode;
}
