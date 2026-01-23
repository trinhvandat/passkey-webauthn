package com.leonard.web_authn.feature.passkey.usecase;

import com.leonard.web_authn.feature.passkey.domain.ChallengeResult;

public interface GenerateChallengeUseCase {
    ChallengeResult execute(String userId, String sessionId);
}
