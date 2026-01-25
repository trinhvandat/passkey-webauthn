package com.leonard.web_authn.feature.authorization.usecase;

import com.leonard.web_authn.feature.authorization.adapter.repository.PermissionRepository;
import com.leonard.web_authn.feature.authorization.adapter.repository.UserRoleRepository;
import com.leonard.web_authn.feature.authorization.domain.UserRole;
import com.leonard.web_authn.feature.authorization.domain.exception.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionCacheService cacheService;

    public boolean hasPermission(String userId, String permissionName) {
        Set<String> permissions = getEffectivePermissions(userId);
        return permissions.contains(permissionName);
    }

    public boolean hasAnyPermission(String userId, String... permissionNames) {
        Set<String> permissions = getEffectivePermissions(userId);
        return Arrays.stream(permissionNames).anyMatch(permissions::contains);
    }

    public boolean hasRole(String userId, String roleName) {
        return getActiveRoles(userId).stream()
                .filter(ur -> ur.getRole() != null)  // Defensive null check
                .anyMatch(ur -> ur.getRole().getName().equals(roleName));
    }

    public void requirePermission(String userId, String permissionName) {
        if (!hasPermission(userId, permissionName)) {
            log.warn("Access denied for user {} - required permission: {}", userId, permissionName);
            throw new AccessDeniedException(permissionName);
        }
    }

    public void requireAnyPermission(String userId, String... permissionNames) {
        if (!hasAnyPermission(userId, permissionNames)) {
            log.warn("Access denied for user {} - required any of: {}", userId, Arrays.toString(permissionNames));
            throw new AccessDeniedException(String.join(", ", permissionNames));
        }
    }

    public void requireRole(String userId, String roleName) {
        if (!hasRole(userId, roleName)) {
            log.warn("Access denied for user {} - required role: {}", userId, roleName);
            throw new AccessDeniedException("Role: " + roleName);
        }
    }

    public Set<String> getEffectivePermissions(String userId) {
        return cacheService.getOrLoad(userId, this::loadPermissions);
    }

    public UserAuthContext buildAuthContext(String userId) {
        List<UserRole> userRoles = getActiveRoles(userId);
        Set<String> roleNames = userRoles.stream()
                .filter(ur -> ur.getRole() != null)  // Defensive null check
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());
        Set<String> permissions = getEffectivePermissions(userId);
        return new UserAuthContext(userId, roleNames, permissions);
    }

    private List<UserRole> getActiveRoles(String userId) {
        return userRoleRepository.findActiveRolesByUserId(userId, LocalDateTime.now());
    }

    private Set<String> loadPermissions(String userId) {
        List<UserRole> activeRoles = getActiveRoles(userId);
        if (activeRoles.isEmpty()) {
            return Collections.emptySet();
        }
        List<Integer> roleIds = activeRoles.stream()
                .map(UserRole::getRoleId)
                .toList();
        return permissionRepository.findPermissionNamesByRoleIds(roleIds);
    }

    public record UserAuthContext(String userId, Set<String> roles, Set<String> permissions) {}
}
