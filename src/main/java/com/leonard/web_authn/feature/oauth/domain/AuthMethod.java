package com.leonard.web_authn.feature.oauth.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_methods")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_user_id")
    private String providerUserId;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "provider_avatar_url")
    private String providerAvatarUrl;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "linked_at", nullable = false)
    @Builder.Default
    private LocalDateTime linkedAt = LocalDateTime.now();

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
