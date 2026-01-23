-- V3: Add recovery codes, session management, and rate limiting tables
-- Also adds account locking fields to users table

-- =====================================================
-- 1. ALTER USERS TABLE - Add account locking fields
-- =====================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_locked BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS lock_reason VARCHAR(100);

-- Index for locked users lookup
CREATE INDEX IF NOT EXISTS idx_users_is_locked ON users(is_locked) WHERE is_locked = true;

-- =====================================================
-- 2. UPDATE PASSKEY_CHALLENGES - Add ADD_CREDENTIAL operation type
-- =====================================================

ALTER TABLE passkey_challenges
    DROP CONSTRAINT IF EXISTS passkey_challenges_operation_type_valid;

ALTER TABLE passkey_challenges
    ADD CONSTRAINT passkey_challenges_operation_type_valid
    CHECK (operation_type IN ('REGISTRATION', 'AUTHENTICATION', 'ADD_CREDENTIAL'));

-- =====================================================
-- 3. RECOVERY_CODES TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS recovery_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT false,
    used_at TIMESTAMP,
    used_from_ip INET,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,

    CONSTRAINT fk_recovery_codes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT recovery_codes_code_hash_unique
        UNIQUE (code_hash)
);

-- Indexes for recovery_codes
CREATE INDEX IF NOT EXISTS idx_recovery_codes_user_id ON recovery_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_recovery_codes_user_unused ON recovery_codes(user_id, is_used) WHERE is_used = false;

COMMENT ON TABLE recovery_codes IS 'Backup codes for account recovery when all passkeys are lost';
COMMENT ON COLUMN recovery_codes.code_hash IS 'BCrypt hashed recovery code (format: XXXX-XXXX)';
COMMENT ON COLUMN recovery_codes.is_used IS 'Each code can only be used once';

-- =====================================================
-- 4. USER_SESSIONS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS user_sessions (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    credential_id VARCHAR(255),
    refresh_token_hash VARCHAR(500),
    ip_address INET,
    user_agent TEXT,
    device_info VARCHAR(255),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    revoke_reason VARCHAR(100),

    CONSTRAINT fk_user_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_sessions_credential
        FOREIGN KEY (credential_id)
        REFERENCES passkey_credentials(id)
        ON DELETE SET NULL
);

-- Indexes for user_sessions
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_token ON user_sessions(refresh_token_hash) WHERE refresh_token_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_active ON user_sessions(user_id, is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_user_sessions_expires ON user_sessions(expires_at);

COMMENT ON TABLE user_sessions IS 'JWT session tracking for authenticated users';
COMMENT ON COLUMN user_sessions.refresh_token_hash IS 'Hashed refresh token for token renewal';
COMMENT ON COLUMN user_sessions.device_info IS 'Parsed user agent (e.g., Chrome on MacOS)';

-- =====================================================
-- 5. LOGIN_ATTEMPTS TABLE (Rate Limiting)
-- =====================================================

CREATE TABLE IF NOT EXISTS login_attempts (
    id BIGSERIAL PRIMARY KEY,
    identifier VARCHAR(255) NOT NULL,
    ip_address INET NOT NULL,
    success BOOLEAN NOT NULL DEFAULT false,
    failure_reason VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for login_attempts
CREATE INDEX IF NOT EXISTS idx_login_attempts_identifier ON login_attempts(identifier);
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip ON login_attempts(ip_address);
CREATE INDEX IF NOT EXISTS idx_login_attempts_created ON login_attempts(created_at);
CREATE INDEX IF NOT EXISTS idx_login_attempts_ip_time ON login_attempts(ip_address, created_at);
CREATE INDEX IF NOT EXISTS idx_login_attempts_identifier_time ON login_attempts(identifier, created_at);

COMMENT ON TABLE login_attempts IS 'Tracks login attempts for rate limiting and brute force protection';
COMMENT ON COLUMN login_attempts.identifier IS 'Username or email that was attempted';
COMMENT ON COLUMN login_attempts.failure_reason IS 'Reason for failure (e.g., invalid_password, user_not_found)';

-- =====================================================
-- 6. UTILITY FUNCTIONS
-- =====================================================

-- Function to cleanup expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM user_sessions
    WHERE expires_at < CURRENT_TIMESTAMP
       OR (revoked_at IS NOT NULL AND revoked_at < CURRENT_TIMESTAMP - INTERVAL '7 days');

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_sessions() IS 'Removes expired and old revoked sessions. Run hourly.';

-- Function to cleanup old login attempts
CREATE OR REPLACE FUNCTION cleanup_old_login_attempts()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM login_attempts
    WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '7 days';

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_old_login_attempts() IS 'Removes login attempts older than 7 days. Run daily.';

-- Function to check rate limits
CREATE OR REPLACE FUNCTION check_rate_limit(
    p_identifier VARCHAR(255),
    p_ip_address INET
)
RETURNS TABLE (
    is_blocked BOOLEAN,
    block_reason VARCHAR(100),
    retry_after_seconds INTEGER
) AS $$
DECLARE
    ip_failures_15min INTEGER;
    identifier_failures_1h INTEGER;
    ip_failures_1h INTEGER;
BEGIN
    -- Count failures in last 15 minutes for this IP
    SELECT COUNT(*) INTO ip_failures_15min
    FROM login_attempts
    WHERE ip_address = p_ip_address
      AND success = false
      AND created_at > CURRENT_TIMESTAMP - INTERVAL '15 minutes';

    -- Count failures in last hour for this identifier
    SELECT COUNT(*) INTO identifier_failures_1h
    FROM login_attempts
    WHERE identifier = p_identifier
      AND success = false
      AND created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour';

    -- Count failures in last hour for this IP
    SELECT COUNT(*) INTO ip_failures_1h
    FROM login_attempts
    WHERE ip_address = p_ip_address
      AND success = false
      AND created_at > CURRENT_TIMESTAMP - INTERVAL '1 hour';

    -- Check rate limits (strictest first)
    IF ip_failures_1h >= 20 THEN
        RETURN QUERY SELECT true, 'IP blocked for excessive attempts'::VARCHAR(100), 86400;
    ELSIF identifier_failures_1h >= 10 THEN
        RETURN QUERY SELECT true, 'Account locked - too many failed attempts'::VARCHAR(100), 3600;
    ELSIF ip_failures_15min >= 5 THEN
        RETURN QUERY SELECT true, 'Too many attempts - please wait'::VARCHAR(100), 900;
    ELSE
        RETURN QUERY SELECT false, NULL::VARCHAR(100), 0;
    END IF;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION check_rate_limit(VARCHAR, INET) IS 'Check if IP or identifier is rate limited. Returns block status and retry time.';

-- Function to cleanup expired recovery codes
CREATE OR REPLACE FUNCTION cleanup_expired_recovery_codes()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM recovery_codes
    WHERE (expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP)
       OR (is_used = true AND used_at < CURRENT_TIMESTAMP - INTERVAL '30 days');

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION cleanup_expired_recovery_codes() IS 'Removes expired and old used recovery codes. Run daily.';

-- =====================================================
-- 7. TRIGGER: Update session last_activity_at
-- =====================================================

CREATE OR REPLACE FUNCTION update_session_activity()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_activity_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Note: This trigger would be called from application code when session is accessed
-- DROP TRIGGER IF EXISTS trigger_session_activity ON user_sessions;
-- CREATE TRIGGER trigger_session_activity
--     BEFORE UPDATE ON user_sessions
--     FOR EACH ROW
--     WHEN (OLD.last_activity_at IS DISTINCT FROM NEW.last_activity_at)
--     EXECUTE FUNCTION update_session_activity();
