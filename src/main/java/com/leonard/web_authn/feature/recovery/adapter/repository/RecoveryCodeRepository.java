package com.leonard.web_authn.feature.recovery.adapter.repository;

import com.leonard.web_authn.feature.recovery.domain.RecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

    List<RecoveryCode> findByUserIdAndIsUsedFalse(String userId);

    Optional<RecoveryCode> findByCodeHash(String codeHash);

    @Query("SELECT COUNT(r) FROM RecoveryCode r WHERE r.userId = :userId AND r.isUsed = false " +
           "AND (r.expiresAt IS NULL OR r.expiresAt > :now)")
    int countUnusedByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(r) FROM RecoveryCode r WHERE r.userId = :userId AND r.isUsed = true")
    int countUsedByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(r) FROM RecoveryCode r WHERE r.userId = :userId")
    int countTotalByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM RecoveryCode r WHERE r.userId = :userId")
    int deleteAllByUserId(@Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM RecoveryCode r WHERE (r.expiresAt IS NOT NULL AND r.expiresAt < :now) " +
           "OR (r.isUsed = true AND r.usedAt < :cutoff)")
    int deleteExpiredCodes(@Param("now") LocalDateTime now, @Param("cutoff") LocalDateTime cutoff);

    boolean existsByUserIdAndIsUsedFalse(String userId);
}
