package com.leonard.web_authn.feature.session.adapter.repository;

import com.leonard.web_authn.feature.session.domain.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    List<UserSession> findByUserIdAndIsActiveTrueOrderByLastActivityAtDesc(String userId);

    Optional<UserSession> findByRefreshTokenHashAndIsActiveTrue(String refreshTokenHash);

    Optional<UserSession> findByIdAndUserIdAndIsActiveTrue(String id, String userId);

    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.isActive = true " +
           "AND s.expiresAt > :now ORDER BY s.lastActivityAt DESC")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = :now, s.revokeReason = :reason " +
           "WHERE s.userId = :userId AND s.isActive = true AND s.id != :currentSessionId")
    int revokeOtherSessions(
            @Param("userId") String userId,
            @Param("currentSessionId") String currentSessionId,
            @Param("now") LocalDateTime now,
            @Param("reason") String reason
    );

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false, s.revokedAt = :now, s.revokeReason = :reason " +
           "WHERE s.userId = :userId AND s.isActive = true")
    int revokeAllUserSessions(
            @Param("userId") String userId,
            @Param("now") LocalDateTime now,
            @Param("reason") String reason
    );

    default void revokeAllSessionsForUser(String userId) {
        revokeAllUserSessions(userId, LocalDateTime.now(), "Account locked");
    }

    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :now WHERE s.id = :sessionId")
    int updateLastActivity(@Param("sessionId") String sessionId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now " +
           "OR (s.revokedAt IS NOT NULL AND s.revokedAt < :cutoff)")
    int deleteExpiredSessions(@Param("now") LocalDateTime now, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.isActive = true " +
           "AND s.expiresAt > :now")
    int countActiveSessions(@Param("userId") String userId, @Param("now") LocalDateTime now);
}
