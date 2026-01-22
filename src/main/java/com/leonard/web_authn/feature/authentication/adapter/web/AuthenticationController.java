package com.leonard.web_authn.feature.authentication.adapter.web;

import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteAuthenticationRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteAuthenticationResponseDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteRegisterRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.CompleteRegisterResponseDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartAuthenticationRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartAuthenticationResponseDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartRegisterRequestDTO;
import com.leonard.web_authn.feature.authentication.adapter.web.dto.StartRegisterResponseDTO;
import com.leonard.web_authn.feature.authentication.usecase.CompleteAuthenticationUseCase;
import com.leonard.web_authn.feature.authentication.usecase.CompleteRegisterUserUseCase;
import com.leonard.web_authn.feature.authentication.usecase.StartAuthenticationUseCase;
import com.leonard.web_authn.feature.authentication.usecase.StartRegisterUserUseCase;
import com.leonard.web_authn.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteAuthenticationMapping.toCommand;
import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteAuthenticationMapping.toResponse;
import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteRegisterMapping.toCommand;
import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteRegisterMapping.toResponse;
import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.StartAuthenticationMapping.toResponse;
import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.StartRegisterMapping.convertToResponse;
import static com.leonard.web_authn.feature.authentication.adapter.web.mapping.StartRegisterMapping.createRegisterCommand;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthenticationController {

    private final StartRegisterUserUseCase startRegisterUserUseCase;
    private final CompleteRegisterUserUseCase completeRegisterUserUseCase;
    private final StartAuthenticationUseCase startAuthenticationUseCase;
    private final CompleteAuthenticationUseCase completeAuthenticationUseCase;

    @PostMapping("/register:start")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StartRegisterResponseDTO> startRegister(
            @Valid @RequestBody StartRegisterRequestDTO request) {
        log.info("Starting registration for user: {}", request.getUsername());
        final var command = createRegisterCommand(request);
        final var result = startRegisterUserUseCase.execute(command);
        return ApiResponse.success(convertToResponse(result));
    }

    @PostMapping("/register:complete")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompleteRegisterResponseDTO> completeRegister(
            @Valid @RequestBody CompleteRegisterRequestDTO request) {
        log.info("Completing registration for user: {}", request.getUsername());
        final var command = com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteRegisterMapping.toCommand(request);
        final var result = completeRegisterUserUseCase.execute(command);
        return ApiResponse.success(com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteRegisterMapping.toResponse(result));
    }

    @PostMapping("/authenticate:start")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StartAuthenticationResponseDTO> startAuthentication(
            @Valid @RequestBody StartAuthenticationRequestDTO request) {
        log.info("Starting authentication for user: {}", request.getUsername());
        final var result = startAuthenticationUseCase.execute(request.getUsername());
        return ApiResponse.success(com.leonard.web_authn.feature.authentication.adapter.web.mapping.StartAuthenticationMapping.toResponse(result));
    }

    @PostMapping("/authenticate:complete")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CompleteAuthenticationResponseDTO> completeAuthentication(
            @Valid @RequestBody CompleteAuthenticationRequestDTO request) {
        log.info("Completing authentication");
        final var command = com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteAuthenticationMapping.toCommand(request);
        final var result = completeAuthenticationUseCase.execute(command);
        return ApiResponse.success(com.leonard.web_authn.feature.authentication.adapter.web.mapping.CompleteAuthenticationMapping.toResponse(result));
    }
}
