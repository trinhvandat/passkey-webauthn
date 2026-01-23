package com.leonard.web_authn.feature.passkey.usecase;

public interface ValidateChallengeUseCase {
    boolean execute(String challenge);
}
