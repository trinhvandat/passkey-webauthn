package com.leonard.web_authn.feature.user.usecase.impl;

import com.leonard.web_authn.feature.user.usecase.CheckEmailRegisteredUseCase;
import com.leonard.web_authn.feature.user.adapter.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckEmailRegisteredUseCaseImpl implements CheckEmailRegisteredUseCase {

    private final UserRepository userRepository;

    @Override
    public boolean execute(String email) {
        return userRepository.existsByEmail(email);
    }
}
