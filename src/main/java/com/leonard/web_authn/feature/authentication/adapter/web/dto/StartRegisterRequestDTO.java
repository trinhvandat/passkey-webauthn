package com.leonard.web_authn.feature.authentication.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class StartRegisterRequestDTO {
    @NotBlank
    private String username;
    @NotBlank
    private String displayName;
    @NotBlank
    private String email;
}
