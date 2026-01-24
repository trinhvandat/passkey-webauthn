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
import com.leonard.web_authn.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<PasskeyInfoDTO>> listPasskeys(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Listing passkeys for user: {}", user.userId());
        List<PasskeyInfoDTO> passkeys = passkeyManagementService.listPasskeys(user.userId()).stream()
                .map(this::toDTO)
                .toList();
        return ApiResponse.success(passkeys);
    }

    @GetMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PasskeyInfoDTO> getPasskey(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String credentialId) {
        log.info("Getting passkey: {} for user: {}", credentialId, user.userId());
        PasskeyInfoDTO passkey = toDTO(passkeyManagementService.getPasskey(user.userId(), credentialId));
        return ApiResponse.success(passkey);
    }

    @PatchMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PasskeyInfoDTO> renamePasskey(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String credentialId,
            @Valid @RequestBody RenamePasskeyRequestDTO request) {
        log.info("Renaming passkey: {} for user: {}", credentialId, user.userId());
        PasskeyInfoDTO passkey = toDTO(passkeyManagementService.renamePasskey(user.userId(), credentialId, request.getName()));
        return ApiResponse.success(passkey);
    }

    @DeleteMapping("/{credentialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePasskey(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String credentialId) {
        log.info("Deleting passkey: {} for user: {}", credentialId, user.userId());
        passkeyManagementService.deletePasskey(user.userId(), credentialId);
    }

    @PostMapping("/revoke-all")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RevokeAllResponse> revokeAllPasskeys(
            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("Revoking all passkeys for user: {}", user.userId());
        int count = passkeyManagementService.revokeAllPasskeys(user.userId());
        return ApiResponse.success(new RevokeAllResponse(count));
    }

    @PostMapping("/add:start")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StartAddPasskeyResponseDTO> startAddPasskey(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody(required = false) StartAddPasskeyRequestDTO request) {
        String deviceName = request != null ? request.getDeviceName() : null;
        log.info("Starting add passkey for user: {}, deviceName: {}", user.userId(), deviceName);
        ChallengeResult result = startAddPasskeyUseCase.execute(user.userId(), deviceName);
        return ApiResponse.success(toStartAddPasskeyResponse(result));
    }

    @PostMapping("/add:complete")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CompleteAddPasskeyResponseDTO> completeAddPasskey(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CompleteAddPasskeyRequestDTO request) {
        log.info("Completing add passkey for user: {}", user.userId());
        CompleteAddPasskeyCommand command = CompleteAddPasskeyCommand.builder()
                .userId(user.userId())
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
