package com.leonard.web_authn.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringUtils {
    public static boolean isBlank(String input) {
        return Objects.isNull(input) || input.isBlank();
    }
}
