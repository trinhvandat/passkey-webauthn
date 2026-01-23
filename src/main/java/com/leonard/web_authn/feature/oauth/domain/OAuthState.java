package com.leonard.web_authn.feature.oauth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String state;

    @Column(nullable = false)
    private String provider;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "redirect_uri")
    private String redirectUri;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    public boolean isValid() {
        return !isUsed && expiresAt.isAfter(LocalDateTime.now());
    }
}
