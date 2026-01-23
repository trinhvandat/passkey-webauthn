-- =====================================================
-- V5: Add RBAC (Role-Based Access Control) tables
-- =====================================================

-- 1. ROLES TABLE
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN NOT NULL DEFAULT false,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_roles_active ON roles(is_active) WHERE is_active = true;

-- 2. PERMISSIONS TABLE
CREATE TABLE permissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(150) NOT NULL,
    description TEXT,
    resource VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permissions_name ON permissions(name);
CREATE INDEX idx_permissions_resource ON permissions(resource);

-- 3. ROLE_PERMISSIONS TABLE (Many-to-Many)
CREATE TABLE role_permissions (
    role_id INTEGER NOT NULL,
    permission_id INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (role_id, permission_id),

    CONSTRAINT fk_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- 4. USER_ROLES TABLE (Many-to-Many)
CREATE TABLE user_roles (
    user_id VARCHAR(255) NOT NULL,
    role_id INTEGER NOT NULL,
    assigned_by VARCHAR(255),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
CREATE INDEX idx_user_roles_expires ON user_roles(expires_at) WHERE expires_at IS NOT NULL;

-- 5. ROLE AUDIT LOG
CREATE TABLE role_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    role_id INTEGER NOT NULL,
    action VARCHAR(20) NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT role_audit_action_valid CHECK (action IN ('ASSIGNED', 'REVOKED'))
);

CREATE INDEX idx_role_audit_user ON role_audit_log(user_id);
CREATE INDEX idx_role_audit_created ON role_audit_log(created_at);

-- =====================================================
-- SEED DEFAULT ROLES
-- =====================================================
INSERT INTO roles (name, display_name, description, is_system) VALUES
    ('ADMIN', 'Administrator', 'Full system access', true),
    ('MODERATOR', 'Moderator', 'User management and content moderation', true),
    ('USER', 'User', 'Standard authenticated user', true);

-- =====================================================
-- SEED DEFAULT PERMISSIONS
-- =====================================================
INSERT INTO permissions (name, display_name, resource, action, description) VALUES
    ('profile:read', 'View Own Profile', 'profile', 'read', 'View own profile'),
    ('profile:write', 'Edit Own Profile', 'profile', 'write', 'Edit own profile'),
    ('passkey:read', 'View Passkeys', 'passkey', 'read', 'View own passkeys'),
    ('passkey:write', 'Manage Passkeys', 'passkey', 'write', 'Add/rename/delete own passkeys'),
    ('passkey:admin', 'Admin Passkeys', 'passkey', 'admin', 'Manage any user passkeys'),
    ('session:read', 'View Sessions', 'session', 'read', 'View own sessions'),
    ('session:write', 'Manage Sessions', 'session', 'write', 'Revoke own sessions'),
    ('session:admin', 'Admin Sessions', 'session', 'admin', 'View/revoke any session'),
    ('recovery:read', 'View Recovery Codes', 'recovery', 'read', 'View recovery code status'),
    ('recovery:write', 'Manage Recovery Codes', 'recovery', 'write', 'Generate new recovery codes'),
    ('user:read', 'View Users', 'user', 'read', 'View user profiles'),
    ('user:write', 'Edit Users', 'user', 'write', 'Modify user profiles'),
    ('user:delete', 'Delete Users', 'user', 'delete', 'Delete user accounts'),
    ('user:lock', 'Lock Users', 'user', 'lock', 'Lock/unlock user accounts'),
    ('admin:dashboard', 'Admin Dashboard', 'admin', 'dashboard', 'Access admin dashboard'),
    ('admin:roles', 'Manage Roles', 'admin', 'roles', 'Create/edit/delete roles'),
    ('admin:audit', 'View Audit Logs', 'admin', 'audit', 'View all system audit logs'),
    ('admin:settings', 'System Settings', 'admin', 'settings', 'Manage system settings');

-- =====================================================
-- ASSIGN PERMISSIONS TO ROLES
-- =====================================================

-- ADMIN gets ALL permissions
INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN';

-- MODERATOR gets user management + audit
INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id FROM roles r, permissions p
    WHERE r.name = 'MODERATOR'
    AND p.name IN ('profile:read', 'profile:write', 'passkey:read', 'passkey:write',
                   'session:read', 'session:write', 'recovery:read', 'recovery:write',
                   'user:read', 'user:write', 'user:lock', 'session:admin', 'admin:audit');

-- USER gets self-management permissions
INSERT INTO role_permissions (role_id, permission_id)
    SELECT r.id, p.id FROM roles r, permissions p
    WHERE r.name = 'USER'
    AND p.name IN ('profile:read', 'profile:write', 'passkey:read', 'passkey:write',
                   'session:read', 'session:write', 'recovery:read', 'recovery:write');

-- =====================================================
-- ASSIGN DEFAULT USER ROLE TO ALL EXISTING USERS
-- =====================================================
INSERT INTO user_roles (user_id, role_id, assigned_by)
    SELECT u.id, r.id, 'SYSTEM'
    FROM users u, roles r
    WHERE r.name = 'USER';
