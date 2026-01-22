package com.leonard.web_authn.feature.authentication.usecase;

import com.leonard.web_authn.feature.authentication.usecase.command.RegisterUserCommand;
import com.leonard.web_authn.feature.passkey.domain.ChallengeResult;

public interface StartRegisterUserUseCase {
    ChallengeResult execute(RegisterUserCommand command);
}
