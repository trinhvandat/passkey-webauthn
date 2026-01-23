package com.leonard.web_authn.feature.authentication.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class StartAuthenticationRequestDTO {
    @NotBlank
    private String username;
}
