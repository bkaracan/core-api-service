-- =======================================================================
-- Flyway Migration: V1__init_user_sso_schema.sql
-- Enterprise User Management, SSO & Social Federation (Google/GitHub)
-- =======================================================================

-- 1. ROLES & PERMISSIONS TABLOLARI
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS permissions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS roles_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 2. USERS TABLOSU (BaseEntity Dual-ID, Karma Kimlik & Soft Delete)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) DEFAULT NULL, -- Sosyal kullanıcılar için NULL olabilir
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, PENDING_VERIFICATION, LOCKED, DELETED
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL
);

CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email)) WHERE deleted = false;
CREATE INDEX idx_users_public_id ON users (public_id);
CREATE INDEX idx_users_status ON users (status);

-- 3. USERS_ROLES TABLOSU
CREATE TABLE IF NOT EXISTS users_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 4. USER_SOCIAL_ACCOUNTS TABLOSU (Google & GitHub Federasyon Bağlantıları)
CREATE TABLE IF NOT EXISTS user_social_accounts (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,            -- 'GOOGLE', 'GITHUB'
    provider_user_id VARCHAR(255) NOT NULL,   -- Google sub veya GitHub id
    provider_email VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL,
    CONSTRAINT uq_social_provider_account UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_social_user_id ON user_social_accounts (user_id);
CREATE INDEX idx_social_provider_lookup ON user_social_accounts (provider, provider_user_id);

-- 5. USER_AUDIT_LOG TABLOSU (Güvenlik Denetim İzi)
CREATE TABLE IF NOT EXISTS user_audit_log (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    auth_type VARCHAR(50) NOT NULL,     -- 'LOCAL', 'SOCIAL_GOOGLE', 'SOCIAL_GITHUB'
    action VARCHAR(100) NOT NULL,       -- 'LOGIN_SUCCESS', 'LOGIN_FAILURE', 'ACCOUNT_LINKED'
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(500),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL
);

CREATE INDEX idx_audit_user_created ON user_audit_log (user_id, created_at DESC);

-- 6. SPRING AUTHORIZATION SERVER STANDART TABLOLARI (PostgreSQL)
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200) DEFAULT NULL,
    client_secret_expires_at TIMESTAMPTZ DEFAULT NULL,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris VARCHAR(1000) DEFAULT NULL,
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL
);

CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000) DEFAULT NULL,
    attributes TEXT DEFAULT NULL,
    state VARCHAR(500) DEFAULT NULL,
    authorization_code_value TEXT DEFAULT NULL,
    authorization_code_issued_at TIMESTAMPTZ DEFAULT NULL,
    authorization_code_expires_at TIMESTAMPTZ DEFAULT NULL,
    authorization_code_metadata TEXT DEFAULT NULL,
    access_token_value TEXT DEFAULT NULL,
    access_token_issued_at TIMESTAMPTZ DEFAULT NULL,
    access_token_expires_at TIMESTAMPTZ DEFAULT NULL,
    access_token_metadata TEXT DEFAULT NULL,
    access_token_type VARCHAR(100) DEFAULT NULL,
    access_token_scopes VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value TEXT DEFAULT NULL,
    oidc_id_token_issued_at TIMESTAMPTZ DEFAULT NULL,
    oidc_id_token_expires_at TIMESTAMPTZ DEFAULT NULL,
    oidc_id_token_metadata TEXT DEFAULT NULL,
    refresh_token_value TEXT DEFAULT NULL,
    refresh_token_issued_at TIMESTAMPTZ DEFAULT NULL,
    refresh_token_expires_at TIMESTAMPTZ DEFAULT NULL,
    refresh_token_metadata TEXT DEFAULT NULL,
    user_code_value TEXT DEFAULT NULL,
    user_code_issued_at TIMESTAMPTZ DEFAULT NULL,
    user_code_expires_at TIMESTAMPTZ DEFAULT NULL,
    user_code_metadata TEXT DEFAULT NULL,
    device_code_value TEXT DEFAULT NULL,
    device_code_issued_at TIMESTAMPTZ DEFAULT NULL,
    device_code_expires_at TIMESTAMPTZ DEFAULT NULL,
    device_code_metadata TEXT DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

-- 7. SEED ROLLER VE PERMISSION'LAR
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Standart Sistem Kullanıcısı'),
    ('ROLE_ADMIN', 'Sistem Yöneticisi')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (name, description) VALUES
    ('user:read', 'Kullanıcı verilerini okuma yetkisi'),
    ('user:write', 'Kullanıcı verilerini yazma/güncelleme yetkisi')
ON CONFLICT (name) DO NOTHING;

-- ROLE_ADMIN rolüne yetkileri bağla
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
