package com.leonard.web_authn.feature.passkey.adapter.web;

import com.leonard.web_authn.feature.passkey.adapter.web.dto.CompleteAddPasskeyRequestDTO;
import com.leonard.web_authn.feature.passkey.adapter.web.dto.CompleteAddPasskeyResponseDTO;
import com.leonard.web_authn.feature.passkey.adapter.web.dto.PasskeyInfoDTO;
import com.leonard.web_authn.feature.passkey.adapter.web.dto.RenamePasskeyRequestDTO;
import com.leonard.web_authn.feature.passkey.adapter.web.dto.StartAddPasskeyRequestDTO;
import com.leonard.web_authn.feature.passkey.adapter.web.dto.StartAddPasskeyResponseDTO;
import com.leonard.web_authn.feature.passkey.domain.ChallengeResult;
import com.leonard.web_authn.feature.passkey.usecase.CompleteAddPasskeyUseCase;
import com.leonard.web_authn.feature.passkey.usecase.PasskeyManagementService;
import com.leonard.web_authn.feature.passkey.usecase.StartAddPasskeyUseCase;
import com.leonard.web_authn.feature.passkey.usecase.command.CompleteAddPasskeyCommand;
import com.leonard.web_authn.shared.dto.ApiResponse;
import com.leonard.web_authn.shared.web.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/passkeys")
@RequiredArgsConstructor
@Slf4j
public class PasskeyController {

    private final PasskeyManagementService passkeyManagementService;
    private final StartAddPasskeyUseCase startAddPasskeyUseCase;
    private final CompleteAddPasskeyUseCase completeAddPasskeyUseCase;
    private final AuthenticatedUserResolver userResolver;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<PasskeyInfoDTO>> listPasskeys(HttpServletRequest request) {
        String userId = userResolver.requireUserId(request);
        log.info("Listing passkeys for user: {}", userId);
        List<PasskeyInfoDTO> passkeys = passkeyManagementService.listPasskeys(userId).stream()
                .map(this::toDTO)
                .toList();
        return ApiResponse.success(passkeys);
    }

    @GetMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PasskeyInfoDTO> getPasskey(
            HttpServletRequest request,
            @PathVariable String credentialId) {
        String userId = userResolver.requireUserId(request);
        log.info("Getting passkey: {} for user: {}", credentialId, userId);
        PasskeyInfoDTO passkey = toDTO(passkeyManagementService.getPasskey(userId, credentialId));
        return ApiResponse.success(passkey);
    }

    @PatchMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PasskeyInfoDTO> renamePasskey(
            HttpServletRequest httpRequest,
            @PathVariable String credentialId,
            @Valid @RequestBody RenamePasskeyRequestDTO request) {
        String userId = userResolver.requireUserId(httpRequest);
        log.info("Renaming passkey: {} for user: {}", credentialId, userId);
        PasskeyInfoDTO passkey = toDTO(passkeyManagementService.renamePasskey(userId, credentialId, request.getName()));
        return ApiResponse.success(passkey);
    }

    @DeleteMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePasskey(
            HttpServletRequest request,
            @PathVariable String credentialId) {
        String userId = userResolver.requireUserId(request);
        log.info("Deleting passkey: {} for user: {}", credentialId, userId);
        passkeyManagementService.deletePasskey(userId, credentialId);
    }

    @PostMapping("/revoke-all")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RevokeAllResponse> revokeAllPasskeys(HttpServletRequest request) {
        String userId = userResolver.requireUserId(request);
        log.info("Revoking all passkeys for user: {}", userId);
        int count = passkeyManagementService.revokeAllPasskeys(userId);
        return ApiResponse.success(new RevokeAllResponse(count));
    }

    @PostMapping("/add:start")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StartAddPasskeyResponseDTO> startAddPasskey(
            HttpServletRequest httpRequest,
            @RequestBody(required = false) StartAddPasskeyRequestDTO request) {
        String userId = userResolver.requireUserId(httpRequest);
        String deviceName = request != null ? request.getDeviceName() : null;
        log.info("Starting add passkey for user: {}, deviceName: {}", userId, deviceName);
        ChallengeResult result = startAddPasskeyUseCase.execute(userId, deviceName);
        return ApiResponse.success(toStartAddPasskeyResponse(result));
    }

    @PostMapping("/add:complete")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompleteAddPasskeyResponseDTO> completeAddPasskey(
            HttpServletRequest httpRequest,
            @Valid @RequestBody CompleteAddPasskeyRequestDTO request) {
        String userId = userResolver.requireUserId(httpRequest);
        log.info("Completing add passkey for user: {}", userId);
        CompleteAddPasskeyCommand command = CompleteAddPasskeyCommand.builder()
                .userId(userId)
                .deviceName(request.getDeviceName())
                .clientDataJSON(request.getClientDataJSON())
                .attestationObject(request.getAttestationObject())
                .transports(request.getTransports())
                .build();
        CompleteAddPasskeyUseCase.AddPasskeyResult result = completeAddPasskeyUseCase.execute(command);
        return ApiResponse.success(toCompleteAddPasskeyResponse(result));
    }

    private StartAddPasskeyResponseDTO toStartAddPasskeyResponse(ChallengeResult result) {
        return StartAddPasskeyResponseDTO.builder()
                .challenge(result.getChallenge())
                .rp(StartAddPasskeyResponseDTO.Rp.builder()
                        .name(result.getRpName())
                        .id(result.getRpId())
                        .build())
                .user(StartAddPasskeyResponseDTO.User.builder()
                        .id(result.getUserId())
                        .name(result.getUsername())
                        .displayName(result.getUserDisplayName())
                        .build())
                .pubKeyCredParams(result.getPubKeyCreds().stream()
                        .map(alg -> StartAddPasskeyResponseDTO.PubKeyCredParam.builder()
                                .alg(alg)
                                .build())
                        .toList())
                .timeout(result.getTimeout())
                .authenticatorSelection(StartAddPasskeyResponseDTO.AuthenticatorSelection.builder()
                        .authenticatorAttachment(result.getAuthenticatorAttachment())
                        .residentKey(result.getAuthenticatorResidentKey())
                        .userVerification(result.getAuthenticationUserVerification())
                        .build())
                .attestation(result.getAttestation())
                .excludeCredentials(result.getExcludeCredentials() != null
                        ? result.getExcludeCredentials().stream()
                                .map(cred -> StartAddPasskeyResponseDTO.ExcludeCredential.builder()
                                        .id(cred.getId())
                                        .transports(cred.getTransports())
                                        .build())
                                .toList()
                        : null)
                .build();
    }

    private CompleteAddPasskeyResponseDTO toCompleteAddPasskeyResponse(CompleteAddPasskeyUseCase.AddPasskeyResult result) {
        return CompleteAddPasskeyResponseDTO.builder()
                .id(result.id())
                .credentialId(result.credentialId())
                .deviceName(result.deviceName())
                .deviceType(result.deviceType())
                .algorithm(result.algorithm())
                .build();
    }

    private PasskeyInfoDTO toDTO(PasskeyManagementService.PasskeyInfo info) {
        return PasskeyInfoDTO.builder()
                .id(info.id())
                .credentialId(info.credentialId())
                .deviceName(info.deviceName())
                .deviceType(info.deviceType())
                .algorithm(info.algorithm())
                .signCount(info.signCount())
                .backupEligible(info.backupEligible())
                .backupState(info.backupState())
                .transports(info.transports())
                .attestationFormat(info.attestationFormat())
                .createdAt(info.createdAt())
                .lastUsedAt(info.lastUsedAt())
                .build();
    }

    public record RevokeAllResponse(int revokedCount) {}
}
