package com.leonard.web_authn.feature.oauth.adapter.repository;

import com.leonard.web_authn.feature.oauth.domain.AuthMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthMethodRepository extends JpaRepository<AuthMethod, Long> {
    Optional<AuthMethod> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<AuthMethod> findByUserIdAndProvider(String userId, String provider);

    List<AuthMethod> findByUserIdAndIsActiveTrue(String userId);

    boolean existsByUserIdAndProvider(String userId, String provider);
}
