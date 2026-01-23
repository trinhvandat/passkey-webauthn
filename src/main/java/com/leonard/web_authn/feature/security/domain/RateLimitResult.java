package com.leonard.web_authn.feature.security.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitResult {

    private boolean blocked;
    private String reason;
    private int retryAfterSeconds;
    private boolean shouldLockAccount;

    public static RateLimitResult allowed() {
        return RateLimitResult.builder()
                .blocked(false)
                .retryAfterSeconds(0)
                .shouldLockAccount(false)
                .build();
    }

    public static RateLimitResult blocked(String reason, int retryAfterSeconds) {
        return RateLimitResult.builder()
                .blocked(true)
                .reason(reason)
                .retryAfterSeconds(retryAfterSeconds)
                .shouldLockAccount(false)
                .build();
    }

    public static RateLimitResult blocked(String reason, int retryAfterSeconds, boolean shouldLockAccount) {
        return RateLimitResult.builder()
                .blocked(true)
                .reason(reason)
                .retryAfterSeconds(retryAfterSeconds)
                .shouldLockAccount(shouldLockAccount)
                .build();
    }
}
