package com.leonard.web_authn.feature.oauth.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OAuthCallbackRequestDTO {
    @NotBlank
    private String code;

    @NotBlank
    private String state;
}
