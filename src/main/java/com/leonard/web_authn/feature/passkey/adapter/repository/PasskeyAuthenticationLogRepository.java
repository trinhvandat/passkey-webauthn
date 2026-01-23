package com.leonard.web_authn.feature.passkey.adapter.repository;

import com.leonard.web_authn.feature.passkey.domain.PasskeyAuthenticationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PasskeyAuthenticationLogRepository extends JpaRepository<PasskeyAuthenticationLog, Long> {

    Page<PasskeyAuthenticationLog> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Query("SELECT l FROM PasskeyAuthenticationLog l WHERE l.userId = :userId AND l.success = :success ORDER BY l.createdAt DESC")
    Page<PasskeyAuthenticationLog> findByUserIdAndSuccess(
            @Param("userId") String userId,
            @Param("success") Boolean success,
            Pageable pageable);
}
