package com.leonard.web_authn.feature.security.domain.exception;

import com.leonard.web_authn.shared.exception.BaseException;
import com.leonard.web_authn.shared.exception.ErrorCode;

public class RateLimitExceededException extends BaseException {

    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
        this.retryAfterSeconds = retryAfterSeconds;
        addParam("retryAfter", String.valueOf(retryAfterSeconds));
    }

    public RateLimitExceededException(String message, int retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
        this.retryAfterSeconds = retryAfterSeconds;
        addMessage(message);
        addParam("retryAfter", String.valueOf(retryAfterSeconds));
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
