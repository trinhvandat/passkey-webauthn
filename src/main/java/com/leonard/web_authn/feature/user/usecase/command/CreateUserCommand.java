package com.leonard.web_authn.feature.user.usecase.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@AllArgsConstructor(staticName = "of")
@ToString
@Getter
public class CreateUserCommand {
    private final String username;
    private final String email;
    private final String displayName;

    public boolean isValid() {
        return !isBlank(username) && !isBlank(email) && !isBlank(displayName);
    }
}
