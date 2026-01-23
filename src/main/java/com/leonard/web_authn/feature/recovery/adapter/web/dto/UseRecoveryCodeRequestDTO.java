package com.leonard.web_authn.feature.recovery.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UseRecoveryCodeRequestDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Recovery code is required")
    private String code;
}
