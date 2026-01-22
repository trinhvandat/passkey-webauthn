package com.leonard.web_authn.feature.passkey.usecase;

import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import com.leonard.web_authn.feature.passkey.usecase.command.CreateChallengeCommand;

public interface CreatePasskeyChallengeUseCase {
    PasskeyChallenges execute(CreateChallengeCommand command);
}
