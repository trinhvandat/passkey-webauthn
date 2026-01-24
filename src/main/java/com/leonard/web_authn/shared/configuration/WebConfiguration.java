package com.leonard.web_authn.shared.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * Note: CORS is configured in SecurityConfig to work with Spring Security.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    // CORS moved to SecurityConfig for proper integration with Spring Security
}
