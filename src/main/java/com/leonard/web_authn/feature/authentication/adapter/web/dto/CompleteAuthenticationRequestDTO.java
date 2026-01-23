package com.leonard.web_authn.feature.authentication.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CompleteAuthenticationRequestDTO {
    @NotBlank
    private String credentialId;

    @NotBlank
    private String rawId;

    @NotBlank
    private String clientDataJSON;

    @NotBlank
    private String authenticatorData;

    @NotBlank
    private String signature;

    private String userHandle;
}
