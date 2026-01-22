package com.leonard.web_authn.feature.authentication.usecase.impl;

import com.leonard.web_authn.feature.authentication.usecase.StartRegisterUserUseCase;
import com.leonard.web_authn.feature.authentication.usecase.command.RegisterUserCommand;
import com.leonard.web_authn.feature.passkey.domain.ChallengeResult;
import com.leonard.web_authn.feature.passkey.domain.exception.EmailAlreadyRegisteredException;
import com.leonard.web_authn.feature.passkey.usecase.CreatePasskeyChallengeUseCase;
import com.leonard.web_authn.feature.passkey.usecase.GenerateChallengeUseCase;
import com.leonard.web_authn.feature.user.usecase.CheckEmailRegisteredUseCase;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StartRegisterUserUseCaseImpl implements StartRegisterUserUseCase {

    private final GenerateChallengeUseCase generateChallengeUseCase;
    private final CheckEmailRegisteredUseCase checkEmailRegisteredUseCase;
    private final CreatePasskeyChallengeUseCase createPasskeyChallengeUseCase;

    @Override
    public ChallengeResult execute(RegisterUserCommand command) {
        if (!command.isValid()) {
            log.error("Invalid command request");
            throw new InvalidRequestException();
        }

        final var sessionId = UUID.randomUUID().toString();

        if (checkEmailRegisteredUseCase.execute(command.getEmail())) {
            log.error("Email is already registered.");
            throw new EmailAlreadyRegisteredException();
        }

        final var newUserId = UUID.randomUUID().toString();
        final var challenge = generateChallengeUseCase.execute(newUserId, sessionId);
        challenge.setUsername(command.getUsername());
        challenge.setUserDisplayName(command.getDisplayName());
        return challenge;
    }


}
