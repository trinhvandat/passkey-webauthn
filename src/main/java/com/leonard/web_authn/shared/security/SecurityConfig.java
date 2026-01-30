package com.leonard.web_authn.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for JWT-based authentication.
 * Configures which endpoints are public vs protected.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Disable CSRF - not needed for stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)

                // Configure CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Stateless session management - no server-side sessions
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Configure endpoint authorization
                .authorizeHttpRequests(auth -> auth
                        // Actuator endpoints - Health check
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        // Public endpoints - Authentication flow
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Public endpoints - Session refresh (uses refresh token, not JWT)
                        .requestMatchers(HttpMethod.POST, "/api/v1/sessions/refresh").permitAll()

                        // Public endpoints - Recovery code login
                        .requestMatchers(HttpMethod.POST, "/api/v1/recovery/codes/use").permitAll()

                        // Public endpoints - Account unlock with recovery code
                        .requestMatchers(HttpMethod.POST, "/api/v1/security/account:unlock").permitAll()

                        // Public endpoints - OAuth flow
                        .requestMatchers(HttpMethod.GET, "/api/v1/oauth/providers").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/oauth/*/authorize").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/oauth/*/callback").permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Handle authentication errors
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

}
