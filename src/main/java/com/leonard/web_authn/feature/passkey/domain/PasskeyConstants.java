package com.leonard.web_authn.feature.passkey.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PasskeyConstants {
    public static final int PASSKEY_REGISTER_CHALLENGE_TTL_SECONDS = 300;
    public static final int PASSKEY_CHALLENGE_LENGTH_IN_BYTES = 32;
}
