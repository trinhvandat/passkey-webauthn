package com.leonard.web_authn.feature.authentication.usecase.command;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@Builder
@ToString
@Getter
public class RegisterUserCommand {
    private String username;
    private String displayName;
    private String email;

    public boolean isValid() {
        return !isBlank(username) && !isBlank(displayName) && !isBlank(email);
    }
}
