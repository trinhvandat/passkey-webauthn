-- V4: Change inet columns to varchar for JPA compatibility
-- The inet type causes issues with Hibernate/JPA when inserting String values

-- 1. Change login_attempts.ip_address from inet to varchar
ALTER TABLE login_attempts
    ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::text;

-- 2. Change user_sessions.ip_address from inet to varchar
ALTER TABLE user_sessions
    ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::text;

-- 3. Change recovery_codes.used_from_ip from inet to varchar
ALTER TABLE recovery_codes
    ALTER COLUMN used_from_ip TYPE VARCHAR(45) USING used_from_ip::text;

-- 4. Change passkey_authentication_logs.ip_address from inet to varchar
ALTER TABLE passkey_authentication_logs
    ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::text;

-- 5. Change passkey_challenges.ip_address from inet to varchar
ALTER TABLE passkey_challenges
    ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::text;

-- Note: VARCHAR(45) is sufficient for both IPv4 (max 15 chars) and IPv6 (max 45 chars)

-- Update the check_rate_limit function to work with VARCHAR instead of INET
CREATE OR REPLACE FUNCTION check_rate_limit(
    p_identifier VARCHAR(255),
    p_ip_address VARCHAR(45)
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

COMMENT ON FUNCTION check_rate_limit(VARCHAR, VARCHAR) IS 'Check if IP or identifier is rate limited. Returns block status and retry time.';
