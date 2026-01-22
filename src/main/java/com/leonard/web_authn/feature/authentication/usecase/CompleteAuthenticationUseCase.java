package com.leonard.web_authn.feature.authentication.usecase;

import com.leonard.web_authn.feature.authentication.usecase.command.CompleteAuthenticationCommand;
import com.leonard.web_authn.feature.passkey.domain.AuthenticationResult;

public interface CompleteAuthenticationUseCase {
    AuthenticationResult execute(CompleteAuthenticationCommand command);
}
