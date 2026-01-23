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
import com.leonard.web_authn.shared.web.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AuthenticatedUserResolver userResolver;

    @GetMapping("/providers")
    public ApiResponse<Map<String, Object>> getEnabledProviders() {
        List<String> providers = providerRegistry.getEnabledProviders();
        return ApiResponse.success(Map.of("providers", providers));
    }

    @PostMapping("/{provider}/authorize")
    public ApiResponse<OAuthAuthorizeResponseDTO> startAuthorization(
            HttpServletRequest request,
            @PathVariable String provider) {
        // userId is optional - null for new signups, set for account linking
        String userId = userResolver.resolveUserId(request);

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
            HttpServletRequest request,
            @PathVariable String provider,
            @Valid @RequestBody OAuthCallbackRequestDTO body) {
        String userId = userResolver.requireUserId(request);
        oAuthService.linkProvider(userId, provider, body.getCode(), body.getState());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{provider}/unlink")
    public ApiResponse<Void> unlinkProvider(
            HttpServletRequest request,
            @PathVariable String provider) {
        String userId = userResolver.requireUserId(request);
        oAuthService.unlinkProvider(userId, provider);
        return ApiResponse.success(null);
    }

    @GetMapping("/methods")
    public ApiResponse<List<AuthMethodInfoDTO>> getAuthMethods(HttpServletRequest request) {
        String userId = userResolver.requireUserId(request);
        List<AuthMethod> methods = authMethodRepository.findByUserIdAndIsActiveTrue(userId);

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
