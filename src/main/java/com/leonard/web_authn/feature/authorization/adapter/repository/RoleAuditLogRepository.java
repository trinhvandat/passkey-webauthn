package com.leonard.web_authn.feature.authorization.adapter.repository;

import com.leonard.web_authn.feature.authorization.domain.RoleAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleAuditLogRepository extends JpaRepository<RoleAuditLog, Long> {
    Page<RoleAuditLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<RoleAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
