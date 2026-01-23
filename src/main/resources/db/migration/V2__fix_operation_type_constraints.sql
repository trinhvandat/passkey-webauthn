-- ============================================================================
-- Flyway Migration: V2__fix_operation_type_constraints.sql
-- Description: Fix operation_type constraints to use uppercase enum values
--              and fix passkey_credentials id column type
-- Author: System
-- Date: 2025-01-22
-- ============================================================================

-- Drop old constraints
ALTER TABLE passkey_challenges DROP CONSTRAINT passkey_challenges_operation_type_valid;
ALTER TABLE passkey_authentication_logs DROP CONSTRAINT passkey_auth_logs_operation_type_valid;

-- Add new constraints with uppercase values (matching Java enum)
ALTER TABLE passkey_challenges
    ADD CONSTRAINT passkey_challenges_operation_type_valid
    CHECK (operation_type IN ('REGISTRATION', 'AUTHENTICATION'));

ALTER TABLE passkey_authentication_logs
    ADD CONSTRAINT passkey_auth_logs_operation_type_valid
    CHECK (operation_type IN ('REGISTRATION', 'AUTHENTICATION'));

-- Update the trigger function to use uppercase
CREATE OR REPLACE FUNCTION update_user_last_login()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.success = true AND NEW.operation_type = 'AUTHENTICATION' THEN
        UPDATE users
        SET last_login_at = NEW.created_at
        WHERE id = NEW.user_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Fix passkey_credentials id column: change from BIGSERIAL to VARCHAR for UUID
-- First drop the foreign key constraint from passkey_authentication_logs
ALTER TABLE passkey_authentication_logs DROP CONSTRAINT fk_passkey_auth_logs_credential;

-- Change the id column type in passkey_credentials
ALTER TABLE passkey_credentials
    ALTER COLUMN id DROP DEFAULT,
    ALTER COLUMN id TYPE VARCHAR(255) USING id::VARCHAR(255);

-- Drop the sequence that was created for BIGSERIAL
DROP SEQUENCE IF EXISTS passkey_credentials_id_seq;

-- Change credential_id in passkey_authentication_logs to VARCHAR
ALTER TABLE passkey_authentication_logs
    ALTER COLUMN credential_id TYPE VARCHAR(255) USING credential_id::VARCHAR(255);

-- Recreate the foreign key constraint
ALTER TABLE passkey_authentication_logs
    ADD CONSTRAINT fk_passkey_auth_logs_credential
    FOREIGN KEY (credential_id)
    REFERENCES passkey_credentials(id)
    ON DELETE SET NULL;

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================
