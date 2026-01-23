package com.leonard.web_authn.feature.oauth.adapter.repository;

import com.leonard.web_authn.feature.oauth.domain.OAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OAuthStateRepository extends JpaRepository<OAuthState, Long> {
    Optional<OAuthState> findByStateAndIsUsedFalse(String state);

    @Modifying
    @Query("DELETE FROM OAuthState o WHERE o.expiresAt < :now OR o.isUsed = true")
    int cleanupExpiredStates(@Param("now") LocalDateTime now);
}
