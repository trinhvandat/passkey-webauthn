package com.leonard.web_authn.feature.user.usecase.impl;

import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import com.leonard.web_authn.feature.user.domain.User;
import com.leonard.web_authn.feature.user.usecase.CreateUserUseCase;
import com.leonard.web_authn.feature.user.usecase.command.CreateUserCommand;
import com.leonard.web_authn.shared.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateUserUseCaseImpl implements CreateUserUseCase {

    private final UserRepository userRepository;

    @Override
    public User execute(CreateUserCommand command) {
        if (!command.isValid()) {
            log.error("CreateUserCommand invalid");
            throw new InvalidRequestException();
        }
        final var user = buildUser(command);
        return userRepository.save(user);
    }

    private User buildUser(CreateUserCommand command) {
        return User.builder()
                .email(command.getEmail())
                .displayName(command.getDisplayName())
                .username(command.getUsername())
                .build();
    }
}
