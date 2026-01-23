package com.leonard.web_authn.feature.passkey.adapter.repository;

import com.leonard.web_authn.feature.passkey.domain.PasskeyCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasskeyCredentialRepository extends JpaRepository<PasskeyCredentials, String> {

    List<PasskeyCredentials> findByUserIdAndIsActiveTrue(String userId);

    Optional<PasskeyCredentials> findByCredentialIdAndIsActiveTrue(byte[] credentialId);

    /**
     * Check if credential already exists (for duplicate prevention)
     */
    boolean existsByCredentialId(byte[] credentialId);

    /**
     * Find credential by ID and verify it belongs to specific user
     */
    Optional<PasskeyCredentials> findByCredentialIdAndUserIdAndIsActiveTrue(byte[] credentialId, String userId);
}
