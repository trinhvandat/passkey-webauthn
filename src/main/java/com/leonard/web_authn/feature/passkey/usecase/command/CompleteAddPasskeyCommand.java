package com.leonard.web_authn.feature.passkey.usecase.command;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import static com.leonard.web_authn.shared.utils.StringUtils.isBlank;

@Data
@Builder
public class CompleteAddPasskeyCommand {
    private String userId;
    private String deviceName;
    private String clientDataJSON;
    private String attestationObject;
    private List<String> transports;

    public boolean isValid() {
        return !isBlank(userId)
                && !isBlank(clientDataJSON)
                && !isBlank(attestationObject);
    }
}
