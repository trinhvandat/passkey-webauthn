package com.leonard.web_authn.feature.authentication.usecase.command;

import lombok.Builder;
import lombok.Data;

import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@Builder
@Data
public class CompleteAuthenticationCommand {
    private String credentialId;
    private String rawId;
    private String clientDataJSON;
    private String authenticatorData;
    private String signature;
    private String userHandle;

    public boolean isValid() {
        return !isBlank(credentialId)
                && !isBlank(rawId)
                && !isBlank(clientDataJSON)
                && !isBlank(authenticatorData)
                && !isBlank(signature);
    }
}
