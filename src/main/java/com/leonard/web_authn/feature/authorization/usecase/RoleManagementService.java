package com.leonard.web_authn.feature.authorization.usecase;

import com.leonard.web_authn.feature.authorization.adapter.repository.RoleAuditLogRepository;
import com.leonard.web_authn.feature.authorization.adapter.repository.RoleRepository;
import com.leonard.web_authn.feature.authorization.adapter.repository.UserRoleRepository;
import com.leonard.web_authn.feature.authorization.domain.Role;
import com.leonard.web_authn.feature.authorization.domain.RoleAuditLog;
import com.leonard.web_authn.feature.authorization.domain.UserRole;
import com.leonard.web_authn.feature.authorization.domain.UserRoleId;
import com.leonard.web_authn.feature.authorization.domain.exception.RoleAlreadyAssignedException;
import com.leonard.web_authn.feature.authorization.domain.exception.RoleNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleAuditLogRepository roleAuditLogRepository;
    private final PermissionCacheService permissionCacheService;

    public List<Role> listAllRoles() {
        return roleRepository.findByIsActiveTrue();
    }

    public Role getRoleById(Integer roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(RoleNotFoundException::new);
    }

    public List<UserRole> getUserRoles(String userId) {
        return userRoleRepository.findActiveRolesByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public void assignRole(String userId, Integer roleId, String performedBy) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(RoleNotFoundException::new);

        UserRoleId id = new UserRoleId(userId, roleId);
        if (userRoleRepository.existsById(id)) {
            throw new RoleAlreadyAssignedException();
        }

        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(roleId)
                .assignedBy(performedBy)
                .assignedAt(LocalDateTime.now())
                .build();
        userRoleRepository.save(userRole);

        RoleAuditLog auditLog = RoleAuditLog.builder()
                .userId(userId)
                .roleId(roleId)
                .action("ASSIGNED")
                .performedBy(performedBy)
                .createdAt(LocalDateTime.now())
                .build();
        roleAuditLogRepository.save(auditLog);

        permissionCacheService.evict(userId);

        log.info("Role '{}' assigned to user '{}' by '{}'", role.getName(), userId, performedBy);
    }

    @Transactional
    public void revokeRole(String userId, Integer roleId, String performedBy, String reason) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(RoleNotFoundException::new);

        UserRoleId id = new UserRoleId(userId, roleId);
        if (!userRoleRepository.existsById(id)) {
            throw new RoleNotFoundException();
        }

        // Prevent removing the last admin
        if ("ADMIN".equals(role.getName())) {
            long adminCount = userRoleRepository.countActiveUsersByRoleId(roleId, LocalDateTime.now());
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot remove the last admin");
            }
        }

        userRoleRepository.deleteById(id);

        RoleAuditLog auditLog = RoleAuditLog.builder()
                .userId(userId)
                .roleId(roleId)
                .action("REVOKED")
                .performedBy(performedBy)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build();
        roleAuditLogRepository.save(auditLog);

        permissionCacheService.evict(userId);

        log.info("Role '{}' revoked from user '{}' by '{}'", role.getName(), userId, performedBy);
    }
}
