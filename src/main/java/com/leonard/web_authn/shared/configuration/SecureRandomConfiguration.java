package com.leonard.web_authn.shared.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Configuration
@Slf4j
public class SecureRandomConfiguration {

    @Bean
    public SecureRandom secureRandom() {
        SecureRandom secureRandomInstance;
        try {
            // CRITICAL: Use NativePRNGNonBlocking hoặc để OS chọn best algorithm
            // NativePRNGNonBlocking: Fast, non-blocking, secure enough cho challenges
            secureRandomInstance = SecureRandom.getInstance("NativePRNGNonBlocking");

            log.info("SecureRandom initialized: algorithm={}, provider={}",
                    secureRandomInstance.getAlgorithm(),
                    secureRandomInstance.getProvider().getName());

        } catch (NoSuchAlgorithmException e) {
            // Fallback to default SecureRandom (still secure)
            log.warn("NativePRNGNonBlocking not available, using default SecureRandom");
            secureRandomInstance = new SecureRandom();
        }
        return secureRandomInstance;
    }
}
