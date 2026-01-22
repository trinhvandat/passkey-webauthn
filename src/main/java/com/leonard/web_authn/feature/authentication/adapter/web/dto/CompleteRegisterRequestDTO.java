package com.leonard.web_authn.feature.authentication.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class CompleteRegisterRequestDTO {
    @NotBlank
    private String username;

    private String displayName;

    @NotBlank
    private String email;

    @NotBlank
    private String credentialId;

    @NotBlank
    private String rawId;

    @NotBlank
    private String clientDataJSON;

    @NotBlank
    private String attestationObject;

    private List<String> transports;
}
