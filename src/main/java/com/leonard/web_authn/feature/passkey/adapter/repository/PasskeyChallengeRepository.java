package com.leonard.web_authn.feature.passkey.adapter.repository;

import com.leonard.web_authn.feature.passkey.domain.PasskeyChallenges;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasskeyChallengeRepository extends JpaRepository<PasskeyChallenges, Long> {
    Optional<PasskeyChallenges> findByChallenge(String challenge);
}
