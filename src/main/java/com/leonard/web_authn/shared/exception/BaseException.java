package com.leonard.web_authn.shared.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
public class BaseException extends RuntimeException {
    private final ErrorCode errorCode;
    private String message;
    private Map<String, String> params;

    protected BaseException(ErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
        this.message = null;
        this.params = new HashMap<>();
    }

    protected void addParam(String key, String value) {
        if (Objects.isNull(this.params)) {
            this.params = new HashMap<>();
        }
        this.params.put(key, value);
    }

    protected void addMessage(String message) {
        this.message = message;
    }
}
