package com.leonard.web_authn.feature.oauth.adapter.web.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Builder
@Getter
public class AuthMethodInfoDTO {
    private Long id;
    private String provider;
    private String providerEmail;
    private String providerName;
    private String providerAvatarUrl;
    private Boolean isPrimary;
    private LocalDateTime linkedAt;
    private LocalDateTime lastUsedAt;
}
