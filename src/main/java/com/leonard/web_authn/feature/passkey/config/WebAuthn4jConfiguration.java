package com.leonard.web_authn.feature.passkey.config;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class WebAuthn4jConfiguration {

    @Bean
    public ObjectConverter objectConverter() {
        return new ObjectConverter();
    }

    @Bean
    public WebAuthnManager webAuthnManager(ObjectConverter objectConverter) {
        return WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
    }

    @Bean
    public List<COSEAlgorithmIdentifier> supportedAlgorithms() {
        return List.of(
                COSEAlgorithmIdentifier.ES256,
                COSEAlgorithmIdentifier.RS256,
                COSEAlgorithmIdentifier.EdDSA
        );
    }
}
