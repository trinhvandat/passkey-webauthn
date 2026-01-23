package com.leonard.web_authn.feature.passkey.usecase;

import com.leonard.web_authn.feature.passkey.usecase.command.CompleteAddPasskeyCommand;

public interface CompleteAddPasskeyUseCase {
    AddPasskeyResult execute(CompleteAddPasskeyCommand command);

    record AddPasskeyResult(
            String id,
            String credentialId,
            String deviceName,
            String deviceType,
            String algorithm
    ) {}
}
