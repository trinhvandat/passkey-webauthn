package com.leonard.web_authn.feature.passkey.usecase.impl;

import com.leonard.web_authn.feature.passkey.adapter.repository.PasskeyChallengeRepository;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import com.leonard.web_authn.feature.passkey.usecase.CreatePasskeyChallengeUseCase;
import com.leonard.web_authn.feature.passkey.usecase.command.CreateChallengeCommand;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.leonard.web_authn.feature.passkey.domain.PasskeyConstants.PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS;


@RequiredArgsConstructor
@Service
@Slf4j
@Transactional
public class CreatePasskeyChallengeUseCaseImpl implements CreatePasskeyChallengeUseCase {

    private final PasskeyChallengeRepository passkeyChallengeRepository;

    @Override
    public PasskeyChallenges execute(CreateChallengeCommand command) {
        if (!command.isValid()) {
            log.error("Invalid create challenge command.");
            throw new InvalidRequestException();
        }
        final var challenge = createPasskeyChallenge(command);
        return passkeyChallengeRepository.save(challenge);
    }

    private PasskeyChallenges createPasskeyChallenge(CreateChallengeCommand command) {
        return PasskeyChallenges.builder()
                .challenge(command.getChallengeId())
                .sessionId(command.getSessionId())
                .expiresAt(LocalDateTime.now().plusSeconds(PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS))
                .userId(command.getUserId())
                .build();
    }
}
