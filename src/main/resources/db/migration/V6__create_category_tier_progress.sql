-- =======================================================================
-- Flyway Migration: V6__create_category_tier_progress.sql
-- 4. Yasa Doyurucu Kıl: Kategori Rozetleri & Küme Terfi Sistemi
-- Bronz -> Gümüş -> Altın -> Platin -> Elmas
-- =======================================================================

CREATE TABLE IF NOT EXISTS category_tier_progress (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    total_badges INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL,
    CONSTRAINT uk_category_tier_progress_user_cat UNIQUE (user_id, category)
);

CREATE INDEX idx_category_tier_progress_user ON category_tier_progress(user_id) WHERE deleted = false;
CREATE INDEX idx_category_tier_progress_public_id ON category_tier_progress(public_id);
