-- ============================================================================
-- Flyway Migration: V1__create_webauthn_tables.sql
-- Description: Initial WebAuthn/Passkey database schema
-- Author: System
-- Date: 2025-01-13
-- ============================================================================

-- ============================================================================
-- 1. CREATE USERS TABLE
-- ============================================================================
CREATE TABLE users (
                       id VARCHAR(255) PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) UNIQUE,
                       display_name VARCHAR(255) NOT NULL,
                       is_active BOOLEAN NOT NULL DEFAULT true,
                       is_email_verified BOOLEAN NOT NULL DEFAULT false,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_login_at TIMESTAMP,

                       CONSTRAINT users_username_not_empty CHECK (LENGTH(TRIM(username)) > 0),
                       CONSTRAINT users_display_name_not_empty CHECK (LENGTH(TRIM(display_name)) > 0)
);

CREATE INDEX idx_users_email ON users(email) WHERE email IS NOT NULL;
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_created_at ON users(created_at);

COMMENT ON TABLE users IS 'Stores basic user account information';
COMMENT ON COLUMN users.id IS 'Opaque user identifier (NOT email) - permanent and immutable';
COMMENT ON COLUMN users.username IS 'Unique username - can be email or custom username';
COMMENT ON COLUMN users.last_login_at IS 'Updated automatically via trigger on successful authentication';

-- ============================================================================
-- 2. CREATE PASSKEY_CREDENTIALS TABLE
-- ============================================================================
CREATE TABLE passkey_credentials (
                                     id BIGSERIAL PRIMARY KEY,
                                     user_id VARCHAR(255) NOT NULL,
                                     credential_id BYTEA NOT NULL UNIQUE,
                                     public_key BYTEA NOT NULL,
                                     algorithm INTEGER NOT NULL,
                                     sign_count BIGINT NOT NULL DEFAULT 0,
                                     aaguid BYTEA,
                                     transports TEXT[],
                                     backup_eligible BOOLEAN NOT NULL DEFAULT false,
                                     backup_state BOOLEAN NOT NULL DEFAULT false,
                                     device_name VARCHAR(255),
                                     device_type VARCHAR(50),
                                     attestation_format VARCHAR(50),
                                     attestation_certificate BYTEA,
                                     user_verified BOOLEAN NOT NULL DEFAULT false,
                                     is_active BOOLEAN NOT NULL DEFAULT true,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     last_used_at TIMESTAMP,

                                     CONSTRAINT fk_passkey_credentials_user
                                         FOREIGN KEY (user_id)
                                             REFERENCES users(id)
                                             ON DELETE CASCADE,

                                     CONSTRAINT passkey_credentials_algorithm_valid
                                         CHECK (algorithm IN (-7, -8, -35, -36, -37, -38, -39, -257, -258, -259)),

                                     CONSTRAINT passkey_credentials_sign_count_non_negative
                                         CHECK (sign_count >= 0),

                                     CONSTRAINT passkey_credentials_aaguid_length
                                         CHECK (aaguid IS NULL OR LENGTH(aaguid) = 16)
);

CREATE INDEX idx_passkey_credentials_user_id ON passkey_credentials(user_id);
CREATE INDEX idx_passkey_credentials_credential_id ON passkey_credentials(credential_id);
CREATE INDEX idx_passkey_credentials_user_active ON passkey_credentials(user_id, is_active);
CREATE INDEX idx_passkey_credentials_created_at ON passkey_credentials(created_at);
CREATE INDEX idx_passkey_credentials_last_used_at ON passkey_credentials(last_used_at);

COMMENT ON TABLE passkey_credentials IS 'Stores WebAuthn credentials (public keys) - CRITICAL SECURITY TABLE';
COMMENT ON COLUMN passkey_credentials.credential_id IS 'Unique identifier from authenticator - used for lookup during authentication';
COMMENT ON COLUMN passkey_credentials.public_key IS 'COSE-encoded public key - used to verify signatures';
COMMENT ON COLUMN passkey_credentials.algorithm IS 'COSE algorithm identifier: -7 (ES256), -257 (RS256), -8 (EdDSA)';
COMMENT ON COLUMN passkey_credentials.sign_count IS 'Counter for replay attack detection - MUST increment or stay 0';
COMMENT ON COLUMN passkey_credentials.aaguid IS 'Authenticator Attestation GUID - identifies authenticator model (16 bytes)';
COMMENT ON COLUMN passkey_credentials.backup_eligible IS 'Can credential be backed up (e.g., iCloud Keychain)';
COMMENT ON COLUMN passkey_credentials.backup_state IS 'Is credential currently backed up';

-- ============================================================================
-- 3. CREATE PASSKEY_CHALLENGES TABLE
-- ============================================================================
CREATE TABLE passkey_challenges (
                                    id BIGSERIAL PRIMARY KEY,
                                    challenge VARCHAR(255) NOT NULL UNIQUE,
                                    user_id VARCHAR(255),
                                    operation_type VARCHAR(20) NOT NULL,
                                    session_id VARCHAR(255),
                                    ip_address INET,
                                    user_agent TEXT,
                                    expires_at TIMESTAMP NOT NULL,
                                    is_used BOOLEAN NOT NULL DEFAULT false,
                                    used_at TIMESTAMP,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT passkey_challenges_operation_type_valid
                                        CHECK (operation_type IN ('registration', 'authentication')),

                                    CONSTRAINT passkey_challenges_expires_at_future
                                        CHECK (expires_at > created_at)
);

CREATE INDEX idx_passkey_challenges_challenge ON passkey_challenges(challenge);
CREATE INDEX idx_passkey_challenges_user_id ON passkey_challenges(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_passkey_challenges_expires_at ON passkey_challenges(expires_at);
CREATE INDEX idx_passkey_challenges_is_used ON passkey_challenges(is_used);

COMMENT ON TABLE passkey_challenges IS 'Temporary storage for challenges - auto-cleanup required';
COMMENT ON COLUMN passkey_challenges.challenge IS 'Base64URL-encoded challenge (32 bytes recommended)';
COMMENT ON COLUMN passkey_challenges.expires_at IS 'Challenge expiration time (5 minutes recommended)';
COMMENT ON COLUMN passkey_challenges.is_used IS 'One-time use flag - prevents replay attacks';

-- ============================================================================
-- 4. CREATE PASSKEY_AUTHENTICATION_LOGS TABLE
-- ============================================================================
CREATE TABLE passkey_authentication_logs (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id VARCHAR(255) NOT NULL,
                                             credential_id BIGINT,
                                             operation_type VARCHAR(20) NOT NULL,
                                             success BOOLEAN NOT NULL,
                                             error_code VARCHAR(100),
                                             error_message TEXT,
                                             sign_count_at_time BIGINT,
                                             sign_count_anomaly BOOLEAN DEFAULT false,
                                             ip_address INET,
                                             user_agent TEXT,
                                             country_code CHAR(2),
                                             city VARCHAR(255),
                                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                             CONSTRAINT fk_passkey_auth_logs_user
                                                 FOREIGN KEY (user_id)
                                                     REFERENCES users(id)
                                                     ON DELETE CASCADE,

                                             CONSTRAINT fk_passkey_auth_logs_credential
                                                 FOREIGN KEY (credential_id)
                                                     REFERENCES passkey_credentials(id)
                                                     ON DELETE SET NULL,

                                             CONSTRAINT passkey_auth_logs_operation_type_valid
                                                 CHECK (operation_type IN ('registration', 'authentication'))
);

CREATE INDEX idx_passkey_auth_logs_user_id ON passkey_authentication_logs(user_id);
CREATE INDEX idx_passkey_auth_logs_credential_id ON passkey_authentication_logs(credential_id) WHERE credential_id IS NOT NULL;
CREATE INDEX idx_passkey_auth_logs_created_at ON passkey_authentication_logs(created_at);
CREATE INDEX idx_passkey_auth_logs_success ON passkey_authentication_logs(success);
CREATE INDEX idx_passkey_auth_logs_anomaly ON passkey_authentication_logs(sign_count_anomaly) WHERE sign_count_anomaly = true;
CREATE INDEX idx_passkey_auth_logs_ip ON passkey_authentication_logs(ip_address);

COMMENT ON TABLE passkey_authentication_logs IS 'Audit trail for all authentication attempts - keep for compliance';
COMMENT ON COLUMN passkey_authentication_logs.sign_count_anomaly IS 'Flag for sign count issues - indicates possible cloned authenticator';

-- ============================================================================
-- 5. CREATE UTILITY FUNCTIONS
-- ============================================================================

-- Function to clean up expired challenges
CREATE OR REPLACE FUNCTION cleanup_expired_challenges()
RETURNS INTEGER AS $$
DECLARE
deleted_count INTEGER;
BEGIN
DELETE FROM passkey_challenges
WHERE expires_at < CURRENT_TIMESTAMP
   OR (is_used = true AND used_at < CURRENT_TIMESTAMP - INTERVAL '1 day');

GET DIAGNOSTICS deleted_count = ROW_COUNT;
RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_challenges() IS 'Cleanup expired or used challenges - run periodically via cron';

-- Function to update user's last_login_at
CREATE OR REPLACE FUNCTION update_user_last_login()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.success = true AND NEW.operation_type = 'authentication' THEN
UPDATE users
SET last_login_at = NEW.created_at
WHERE id = NEW.user_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update last_login_at
CREATE TRIGGER trigger_update_user_last_login
    AFTER INSERT ON passkey_authentication_logs
    FOR EACH ROW
    EXECUTE FUNCTION update_user_last_login();

COMMENT ON FUNCTION update_user_last_login() IS 'Automatically updates users.last_login_at on successful authentication';

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for users table
CREATE TRIGGER trigger_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON FUNCTION update_updated_at_column() IS 'Automatically updates updated_at column on row update';

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================