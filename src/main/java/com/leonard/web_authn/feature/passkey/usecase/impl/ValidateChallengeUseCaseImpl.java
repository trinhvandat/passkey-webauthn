package com.leonard.web_authn.feature.passkey.usecase.impl;

import com.leonard.web_authn.feature.passkey.usecase.ValidateChallengeUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidateChallengeUseCaseImpl implements ValidateChallengeUseCase {
    @Override
    public boolean execute(String challenge) {
        if (challenge == null || challenge.isEmpty()) {
            return false;
        }

        if (challenge.length() < 22 || challenge.length() > 200) {
            return false;
        }

        // Check Base64URL format (only A-Z, a-z, 0-9, -, _)
        return challenge.matches("^[A-Za-z0-9_-]+$");
    }
}
