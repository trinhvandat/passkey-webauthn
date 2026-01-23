package com.leonard.web_authn.feature.passkey.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "web-authn")
@Data
public class WebAuthnProperties {
    private String rpName;
    private String rpId;
    private String origin;
    private List<Integer> supportedAlgorithms;
    private Integer timeout;
    private String attestation;
    private String authenticatorAttachment;
    private String residentKey;
    private String userVerification;
}
