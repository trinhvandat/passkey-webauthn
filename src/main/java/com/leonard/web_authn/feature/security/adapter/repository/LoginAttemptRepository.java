package com.leonard.web_authn.feature.security.adapter.repository;

import com.leonard.web_authn.feature.security.domain.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("SELECT COUNT(a) FROM LoginAttempt a WHERE a.ipAddress = :ip AND a.success = false " +
           "AND a.createdAt > :since")
    int countFailedAttemptsByIpSince(@Param("ip") String ipAddress, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM LoginAttempt a WHERE a.identifier = :identifier AND a.success = false " +
           "AND a.createdAt > :since")
    int countFailedAttemptsByIdentifierSince(@Param("identifier") String identifier, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM LoginAttempt a WHERE a.ipAddress = :ip AND a.success = true " +
           "AND a.createdAt > :since")
    int countSuccessfulAttemptsByIpSince(@Param("ip") String ipAddress, @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.createdAt < :cutoff")
    int deleteOldAttempts(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.identifier = :identifier AND a.success = false " +
           "AND a.createdAt > :since")
    int clearFailedAttemptsForIdentifier(@Param("identifier") String identifier, @Param("since") LocalDateTime since);
}
