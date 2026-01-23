-- =====================================================
-- V6: Add OAuth2/Multi-Auth tables
-- =====================================================

-- 1. AUTH_METHODS TABLE - Links multiple auth methods to one user
CREATE TABLE auth_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255),
    provider_email VARCHAR(255),
    provider_name VARCHAR(255),
    provider_avatar_url TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,

    CONSTRAINT fk_auth_methods_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT auth_methods_provider_valid
        CHECK (provider IN ('PASSKEY', 'GOOGLE', 'GITHUB', 'FACEBOOK', 'KEYCLOAK')),

    CONSTRAINT auth_methods_unique_provider
        UNIQUE (user_id, provider),

    CONSTRAINT auth_methods_unique_provider_user
        UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_auth_methods_user ON auth_methods(user_id);
CREATE INDEX idx_auth_methods_provider ON auth_methods(provider, provider_user_id);
CREATE INDEX idx_auth_methods_email ON auth_methods(provider_email);

-- 2. OAUTH_STATES TABLE - CSRF protection for OAuth flows
CREATE TABLE oauth_states (
    id BIGSERIAL PRIMARY KEY,
    state VARCHAR(255) NOT NULL UNIQUE,
    provider VARCHAR(50) NOT NULL,
    user_id VARCHAR(255),
    redirect_uri TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_oauth_states_state ON oauth_states(state);
CREATE INDEX idx_oauth_states_expires ON oauth_states(expires_at);

-- 3. MIGRATE EXISTING PASSKEY USERS TO AUTH_METHODS
INSERT INTO auth_methods (user_id, provider, is_primary, is_active, linked_at)
    SELECT DISTINCT u.id, 'PASSKEY', true, true, u.created_at
    FROM users u
    JOIN passkey_credentials pc ON pc.user_id = u.id AND pc.is_active = true;

-- 4. CLEANUP FUNCTION for expired OAuth states
CREATE OR REPLACE FUNCTION cleanup_expired_oauth_states()
RETURNS void AS $$
BEGIN
    DELETE FROM oauth_states WHERE expires_at < CURRENT_TIMESTAMP OR is_used = true;
END;
$$ LANGUAGE plpgsql;
