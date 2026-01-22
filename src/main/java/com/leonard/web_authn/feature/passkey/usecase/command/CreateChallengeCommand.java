package com.leonard.web_authn.feature.passkey.usecase.command;

import lombok.Builder;
import lombok.Data;

import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@Builder
@Data
public class CreateChallengeCommand {
    private String userId;
    private String sessionId;
    private String challengeId;

    public boolean isValid() {
        return !isBlank(sessionId) && !isBlank(challengeId) && !isBlank(userId);
    }
}
