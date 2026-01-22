package com.leonard.web_authn.feature.passkey.adapter.repository;

import com.leonard.web_authn.feature.passkey.domain.OperationType;
import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasskeyChallengeRepository extends JpaRepository<PasskeyChallenges, Long> {

    Optional<PasskeyChallenges> findByChallenge(String challenge);

    /**
     * Find challenge with pessimistic lock to prevent race conditions
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM PasskeyChallenges c WHERE c.challenge = :challenge")
    Optional<PasskeyChallenges> findByChallengeWithLock(@Param("challenge") String challenge);

    /**
     * Atomic operation to mark challenge as used - returns number of rows updated
     * Only marks as used if: not already used, not expired, and matches operation type
     */
    @Modifying
    @Query("UPDATE PasskeyChallenges c SET c.isUsed = true, c.usedAt = :usedAt " +
           "WHERE c.challenge = :challenge AND c.isUsed = false " +
           "AND c.expiresAt > :now AND c.operationType = :operationType")
    int markChallengeAsUsedAtomic(
            @Param("challenge") String challenge,
            @Param("usedAt") LocalDateTime usedAt,
            @Param("now") LocalDateTime now,
            @Param("operationType") OperationType operationType
    );

    /**
     * Delete expired challenges (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM PasskeyChallenges c WHERE c.expiresAt < :now OR (c.isUsed = true AND c.usedAt < :cutoff)")
    int deleteExpiredChallenges(@Param("now") LocalDateTime now, @Param("cutoff") LocalDateTime cutoff);
}
