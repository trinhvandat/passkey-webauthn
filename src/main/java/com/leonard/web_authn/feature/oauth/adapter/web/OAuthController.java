package com.leonard.web_authn.feature.oauth.adapter.web;

import com.leonard.web_authn.feature.oauth.adapter.repository.AuthMethodRepository;
import com.leonard.web_authn.feature.oauth.adapter.web.dto.AuthMethodInfoDTO;
import com.leonard.web_authn.feature.oauth.adapter.web.dto.OAuthAuthorizeResponseDTO;
import com.leonard.web_authn.feature.oauth.adapter.web.dto.OAuthCallbackRequestDTO;
import com.leonard.web_authn.feature.oauth.adapter.web.dto.OAuthLoginResponseDTO;
import com.leonard.web_authn.feature.oauth.domain.AuthMethod;
import com.leonard.web_authn.feature.oauth.usecase.OAuthProviderRegistry;
import com.leonard.web_authn.feature.oauth.usecase.OAuthService;
import com.leonard.web_authn.shared.dto.ApiResponse;
import com.leonard.web_authn.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/oauth")
@RequiredArgsConstructor
@Slf4j
public class OAuthController {

    private final OAuthService oAuthService;
    private final OAuthProviderRegistry providerRegistry;
    private final AuthMethodRepository authMethodRepository;

    @GetMapping("/providers")
    public ApiResponse<Map<String, Object>> getEnabledProviders() {
        List<String> providers = providerRegistry.getEnabledProviders();
        return ApiResponse.success(Map.of("providers", providers));
    }

    @PostMapping("/{provider}/authorize")
    public ApiResponse<OAuthAuthorizeResponseDTO> startAuthorization(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String provider) {
        // userId is optional - null for new signups, set for account linking
        String userId = user != null ? user.userId() : null;

        String authorizationUrl = oAuthService.startAuthorization(provider, userId);
        OAuthAuthorizeResponseDTO response = OAuthAuthorizeResponseDTO.builder()
                .authorizationUrl(authorizationUrl)
                .build();
        return ApiResponse.success(response);
    }

    @PostMapping("/{provider}/callback")
    public ApiResponse<OAuthLoginResponseDTO> handleCallback(
            @PathVariable String provider,
            @Valid @RequestBody OAuthCallbackRequestDTO body) {
        OAuthService.OAuthResult result = oAuthService.handleCallback(provider, body.getCode(), body.getState());

        OAuthLoginResponseDTO response = OAuthLoginResponseDTO.builder()
                .userId(result.getUserId())
                .username(result.getUsername())
                .email(result.getEmail())
                .displayName(result.getDisplayName())
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .expiresIn(result.getExpiresIn())
                .sessionId(result.getSessionId())
                .build();
        return ApiResponse.success(response);
    }

    @PostMapping("/{provider}/link")
    public ApiResponse<Void> linkProvider(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String provider,
            @Valid @RequestBody OAuthCallbackRequestDTO body) {
        oAuthService.linkProvider(user.userId(), provider, body.getCode(), body.getState());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{provider}/unlink")
    public ApiResponse<Void> unlinkProvider(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String provider) {
        oAuthService.unlinkProvider(user.userId(), provider);
        return ApiResponse.success(null);
    }

    @GetMapping("/methods")
    public ApiResponse<List<AuthMethodInfoDTO>> getAuthMethods(@AuthenticationPrincipal AuthenticatedUser user) {
        List<AuthMethod> methods = authMethodRepository.findByUserIdAndIsActiveTrue(user.userId());

        List<AuthMethodInfoDTO> dtos = methods.stream()
                .map(this::toAuthMethodInfoDTO)
                .toList();
        return ApiResponse.success(dtos);
    }

    private AuthMethodInfoDTO toAuthMethodInfoDTO(AuthMethod method) {
        return AuthMethodInfoDTO.builder()
                .id(method.getId())
                .provider(method.getProvider())
                .providerEmail(method.getProviderEmail())
                .providerName(method.getProviderName())
                .providerAvatarUrl(method.getProviderAvatarUrl())
                .isPrimary(method.getIsPrimary())
                .linkedAt(method.getLinkedAt())
                .lastUsedAt(method.getLastUsedAt())
                .build();
    }
}
