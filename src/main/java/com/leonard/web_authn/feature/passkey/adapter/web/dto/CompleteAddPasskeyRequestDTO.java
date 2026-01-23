package com.leonard.web_authn.feature.passkey.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CompleteAddPasskeyRequestDTO {
    private String deviceName;

    @NotBlank(message = "clientDataJSON is required")
    private String clientDataJSON;

    @NotBlank(message = "attestationObject is required")
    private String attestationObject;

    private List<String> transports;
}
