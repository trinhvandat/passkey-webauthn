package com.leonard.web_authn.feature.oauth.adapter.web.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OAuthAuthorizeResponseDTO {
    private String authorizationUrl;
}
