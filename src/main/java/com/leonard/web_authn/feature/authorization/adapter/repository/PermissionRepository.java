package com.leonard.web_authn.feature.authorization.adapter.repository;

import com.leonard.web_authn.feature.authorization.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    @Query("SELECT p.name FROM Permission p " +
           "JOIN RolePermission rp ON rp.permissionId = p.id " +
           "WHERE rp.roleId IN :roleIds")
    Set<String> findPermissionNamesByRoleIds(@Param("roleIds") List<Integer> roleIds);
}
