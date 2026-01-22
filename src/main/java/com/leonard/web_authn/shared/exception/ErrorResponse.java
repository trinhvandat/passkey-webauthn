package com.leonard.web_authn.shared.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private String code;
    private String message;
    private Map<String, String> params;
    private LocalDateTime timestamp;
}
