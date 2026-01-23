package com.leonard.web_authn.feature.authorization.adapter.web;

import com.leonard.web_authn.feature.authorization.adapter.web.dto.AssignRoleRequestDTO;
import com.leonard.web_authn.feature.authorization.adapter.web.dto.RevokeRoleRequestDTO;
import com.leonard.web_authn.feature.authorization.adapter.web.dto.RoleInfoDTO;
import com.leonard.web_authn.feature.authorization.adapter.web.dto.UserRoleInfoDTO;
import com.leonard.web_authn.feature.authorization.domain.Role;
import com.leonard.web_authn.feature.authorization.domain.UserRole;
import com.leonard.web_authn.feature.authorization.usecase.RoleManagementService;
import com.leonard.web_authn.shared.dto.ApiResponse;
import com.leonard.web_authn.shared.web.AuthenticatedUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleManagementController {

    private final RoleManagementService roleManagementService;
    private final AuthenticatedUserResolver userResolver;

    @GetMapping
    public ApiResponse<List<RoleInfoDTO>> listRoles(HttpServletRequest request) {
        userResolver.requirePermission(request, "admin:roles");

        List<RoleInfoDTO> roles = roleManagementService.listAllRoles().stream()
                .map(this::toRoleInfoDTO)
                .toList();
        return ApiResponse.success(roles);
    }

    @GetMapping("/{roleId}")
    public ApiResponse<RoleInfoDTO> getRole(HttpServletRequest request, @PathVariable Integer roleId) {
        userResolver.requirePermission(request, "admin:roles");

        Role role = roleManagementService.getRoleById(roleId);
        return ApiResponse.success(toRoleInfoDTO(role));
    }

    @GetMapping("/users/{targetUserId}")
    public ApiResponse<UserRoleInfoDTO> getUserRoles(HttpServletRequest request, @PathVariable String targetUserId) {
        userResolver.requirePermission(request, "user:read");

        List<UserRole> userRoles = roleManagementService.getUserRoles(targetUserId);
        UserRoleInfoDTO dto = UserRoleInfoDTO.builder()
                .userId(targetUserId)
                .roles(userRoles.stream().map(this::toRoleAssignment).toList())
                .build();
        return ApiResponse.success(dto);
    }

    @PostMapping("/users/{targetUserId}")
    public ApiResponse<Void> assignRole(
            HttpServletRequest request,
            @PathVariable String targetUserId,
            @Valid @RequestBody AssignRoleRequestDTO body) {
        String adminId = userResolver.requirePermission(request, "admin:roles");

        roleManagementService.assignRole(targetUserId, body.getRoleId(), adminId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/users/{targetUserId}/roles/{roleId}")
    public ApiResponse<Void> revokeRole(
            HttpServletRequest request,
            @PathVariable String targetUserId,
            @PathVariable Integer roleId,
            @RequestBody(required = false) RevokeRoleRequestDTO body) {
        String adminId = userResolver.requirePermission(request, "admin:roles");

        String reason = body != null ? body.getReason() : null;
        roleManagementService.revokeRole(targetUserId, roleId, adminId, reason);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserRoleInfoDTO> getMyRoles(HttpServletRequest request) {
        String userId = userResolver.requireUserId(request);

        List<UserRole> userRoles = roleManagementService.getUserRoles(userId);
        UserRoleInfoDTO dto = UserRoleInfoDTO.builder()
                .userId(userId)
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
