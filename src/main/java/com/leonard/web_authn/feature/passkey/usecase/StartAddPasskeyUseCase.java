package com.leonard.web_authn.feature.passkey.usecase;

import com.leonard.web_authn.feature.passkey.domain.ChallengeResult;

public interface StartAddPasskeyUseCase {
    ChallengeResult execute(String userId, String deviceName);
}
