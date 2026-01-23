package com.leonard.web_authn.feature.security.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnlockAccountRequestDTO {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Recovery code is required")
    private String recoveryCode;
}
