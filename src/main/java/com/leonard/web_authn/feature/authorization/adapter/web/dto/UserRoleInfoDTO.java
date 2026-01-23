package com.leonard.web_authn.feature.authorization.adapter.web.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
public class UserRoleInfoDTO {
    private String userId;
    private List<RoleAssignment> roles;

    @Builder
    @Getter
    public static class RoleAssignment {
        private Integer roleId;
        private String roleName;
        private String displayName;
        private String assignedBy;
        private LocalDateTime assignedAt;
        private LocalDateTime expiresAt;
    }
}
