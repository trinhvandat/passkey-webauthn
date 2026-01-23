package com.leonard.web_authn.feature.user.usecase;

import com.leonard.web_authn.feature.user.domain.User;
import com.leonard.web_authn.feature.user.usecase.command.CreateUserCommand;

public interface CreateUserUseCase {
    User execute(CreateUserCommand command);
}
