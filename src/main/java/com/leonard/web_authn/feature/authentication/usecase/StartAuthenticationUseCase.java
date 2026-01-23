package com.leonard.web_authn.feature.authentication.usecase;

import com.leonard.web_authn.feature.passkey.domain.AuthenticationChallengeResult;

public interface StartAuthenticationUseCase {
    AuthenticationChallengeResult execute(String username);
}
