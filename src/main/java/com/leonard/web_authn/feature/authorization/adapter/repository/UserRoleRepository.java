package com.leonard.web_authn.feature.authorization.adapter.repository;

import com.leonard.web_authn.feature.authorization.domain.UserRole;
import com.leonard.web_authn.feature.authorization.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role " +
           "WHERE ur.userId = :userId " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    List<UserRole> findActiveRolesByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);

    List<UserRole> findByUserId(String userId);

    @Query("SELECT COUNT(ur) FROM UserRole ur " +
           "WHERE ur.roleId = :roleId " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :now)")
    long countActiveUsersByRoleId(@Param("roleId") Integer roleId, @Param("now") LocalDateTime now);

    void deleteByUserIdAndRoleId(String userId, Integer roleId);
}
