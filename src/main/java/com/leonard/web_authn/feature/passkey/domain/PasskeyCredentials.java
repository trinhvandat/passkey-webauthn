package com.leonard.web_authn.feature.passkey.domain;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "passkey_credentials")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PasskeyCredentials {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "credential_id", nullable = false, unique = true)
    private byte[] credentialId;

    @Column(name = "public_key", nullable = false)
    private byte[] publicKey;

    @Column(name = "algorithm", nullable = false)
    private Integer algorithm;

    @Column(name = "sign_count", nullable = false)
    @Builder.Default
    private Long signCount = 0L;

    @Column(name = "aaguid")
    private byte[] aaguid;

    @Column(name = "transports")
    private List<String> transports;

    @Column(name = "backup_eligible", nullable = false)
    @Builder.Default
    private Boolean backupEligible = false;

    @Column(name = "backup_state", nullable = false)
    @Builder.Default
    private Boolean backupState = false;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "attestation_format")
    private String attestationFormat;

    @Column(name = "attestation_certificate")
    private byte[] attestationCertificate;

    @Column(name = "user_verified", nullable = false)
    private Boolean userVerified;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
}
