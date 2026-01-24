package com.leonard.web_authn.feature.authorization.adapter.web;

import com.leonard.web_authn.feature.authorization.adapter.web.dto.AssignRoleRequestDTO;
import com.leonard.web_authn.feature.authorization.adapter.web.dto.RevokeRoleRequestDTO;
import com.leonard.web_authn.feature.authorization.adapter.web.dto.RoleInfoDTO;
import com.leonard.web_authn.feature.authorization.adapter.web.dto.UserRoleInfoDTO;
import com.leonard.web_authn.feature.authorization.domain.Role;
import com.leonard.web_authn.feature.authorization.domain.UserRole;
import com.leonard.web_authn.feature.authorization.usecase.RoleManagementService;
import com.leonard.web_authn.shared.dto.ApiResponse;
import com.leonard.web_authn.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleManagementController {

    private final RoleManagementService roleManagementService;

    @GetMapping
    @PreAuthorize("hasAuthority('admin:roles')")
    public ApiResponse<List<RoleInfoDTO>> listRoles() {
        List<RoleInfoDTO> roles = roleManagementService.listAllRoles().stream()
                .map(this::toRoleInfoDTO)
                .toList();
        return ApiResponse.success(roles);
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAuthority('admin:roles')")
    public ApiResponse<RoleInfoDTO> getRole(@PathVariable Integer roleId) {
        Role role = roleManagementService.getRoleById(roleId);
        return ApiResponse.success(toRoleInfoDTO(role));
    }

    @GetMapping("/users/{targetUserId}")
    @PreAuthorize("hasAuthority('user:read')")
    public ApiResponse<UserRoleInfoDTO> getUserRoles(@PathVariable String targetUserId) {
        List<UserRole> userRoles = roleManagementService.getUserRoles(targetUserId);
        UserRoleInfoDTO dto = UserRoleInfoDTO.builder()
                .userId(targetUserId)
                .roles(userRoles.stream().map(this::toRoleAssignment).toList())
                .build();
        return ApiResponse.success(dto);
    }

    @PostMapping("/users/{targetUserId}")
    @PreAuthorize("hasAuthority('admin:roles')")
    public ApiResponse<Void> assignRole(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String targetUserId,
            @Valid @RequestBody AssignRoleRequestDTO body) {
        roleManagementService.assignRole(targetUserId, body.getRoleId(), user.userId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/users/{targetUserId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('admin:roles')")
    public ApiResponse<Void> revokeRole(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String targetUserId,
            @PathVariable Integer roleId,
            @RequestBody(required = false) RevokeRoleRequestDTO body) {
        String reason = body != null ? body.getReason() : null;
        roleManagementService.revokeRole(targetUserId, roleId, user.userId(), reason);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserRoleInfoDTO> getMyRoles(@AuthenticationPrincipal AuthenticatedUser user) {
        List<UserRole> userRoles = roleManagementService.getUserRoles(user.userId());
        UserRoleInfoDTO dto = UserRoleInfoDTO.builder()
                .userId(user.userId())
                .roles(userRoles.stream().map(this::toRoleAssignment).toList())
                .build();
        return ApiResponse.success(dto);
    }

    private RoleInfoDTO toRoleInfoDTO(Role role) {
        return RoleInfoDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .displayName(role.getDisplayName())
                .description(role.getDescription())
                .isSystem(role.getIsSystem())
                .build();
    }

    private UserRoleInfoDTO.RoleAssignment toRoleAssignment(UserRole userRole) {
        return UserRoleInfoDTO.RoleAssignment.builder()
                .roleId(userRole.getRoleId())
                .roleName(userRole.getRole() != null ? userRole.getRole().getName() : null)
                .displayName(userRole.getRole() != null ? userRole.getRole().getDisplayName() : null)
                .assignedBy(userRole.getAssignedBy())
                .assignedAt(userRole.getAssignedAt())
                .expiresAt(userRole.getExpiresAt())
                .build();
    }
}
