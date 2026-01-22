package com.leonard.web_authn.feature.authentication.usecase.command;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@Builder
@Data
public class CompleteRegisterCommand {
    private String username;
    private String displayName;
    private String email;

    private String credentialId;
    private String rawId;
    private String clientDataJSON;
    private String attestationObject;
    private List<String> transports;

    public boolean isValid() {
        return !isBlank(credentialId)
                && !isBlank(rawId)
                && !isBlank(clientDataJSON)
                && !isBlank(attestationObject)
                && !isBlank(username)
                && !isBlank(email);
    }
}
