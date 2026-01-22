package com.leonard.web_authn.feature.authentication.usecase;

import com.leonard.web_authn.feature.authentication.usecase.command.CompleteRegisterCommand;
import com.leonard.web_authn.feature.user.domain.User;

public interface CompleteRegisterUserUseCase {
    User execute(CompleteRegisterCommand command);
}
