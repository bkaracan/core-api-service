-- =======================================================================
-- Flyway Migration: V2__init_behavioral_habits_schema.sql
-- Enterprise Behavioral UX, Atomic Habits & Kaizen PDCA Schema
-- Dual-ID Pattern, Optimistic Locking, Soft Delete & Full Auditing
-- =======================================================================

-- 1. IDENTITIES (James Clear Kimlik Odaklı Takip Tablosu)
CREATE TABLE IF NOT EXISTS identities (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    tagline VARCHAR(255) NOT NULL,
    icon VARCHAR(20) NOT NULL DEFAULT '🎯',
    color VARCHAR(30) NOT NULL DEFAULT '#6366F1',
    level INT NOT NULL DEFAULT 1,
    total_votes INT NOT NULL DEFAULT 0,
    votes_threshold INT NOT NULL DEFAULT 50,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL
);

CREATE INDEX idx_identities_user ON identities(user_id) WHERE deleted = false;
CREATE INDEX idx_identities_public_id ON identities(public_id);

-- 2. HABITS (4 Davranış Değişimi Yasası ile Atomik Alışkanlıklar)
CREATE TABLE IF NOT EXISTS habits (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    identity_id BIGINT REFERENCES identities(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'KARIYER', -- 'ZIHIN', 'BEDEN', 'KARIYER', 'ODAK'
    cue_trigger VARCHAR(255) NOT NULL,              -- 1. Yasa: İşaret (Alışkanlık Demetleme)
    habit_stack_current VARCHAR(255),               -- Demetleme: [Mevcut Alışkanlık]
    habit_stack_new VARCHAR(255),                   -- Demetleme: [Yeni Alışkanlık]
    craving_benefit VARCHAR(255),                   -- 2. Yasa: İstek (Neden Çekici?)
    response_micro_step VARCHAR(255) NOT NULL,      -- 3. Yasa: Tepki (2-Dakika Kuralı Mikro Adımı)
    reward_xp INT NOT NULL DEFAULT 20,              -- 4. Yasa: Doyurucu Kıl (Dopamin & XP Puanı)
    frequency VARCHAR(30) NOT NULL DEFAULT 'DAILY', -- 'DAILY', 'WEEKDAYS', 'WEEKLY'
    target_minutes INT DEFAULT 15,
    current_streak INT NOT NULL DEFAULT 0,
    best_streak INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL
);

CREATE INDEX idx_habits_user ON habits(user_id) WHERE deleted = false;
CREATE INDEX idx_habits_identity ON habits(identity_id) WHERE deleted = false;
CREATE INDEX idx_habits_public_id ON habits(public_id);

-- 3. HABIT_LOGS (Günlük Çetele & Tamamlanma Kayıtları)
CREATE TABLE IF NOT EXISTS habit_logs (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    habit_id BIGINT NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    log_date DATE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT TRUE,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_two_minute_rule BOOLEAN NOT NULL DEFAULT FALSE,
    earned_xp INT NOT NULL DEFAULT 20,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL,
    CONSTRAINT uq_habit_log_daily UNIQUE (habit_id, log_date)
);

CREATE INDEX idx_habit_logs_user_date ON habit_logs(user_id, log_date);
CREATE INDEX idx_habit_logs_habit ON habit_logs(habit_id);
CREATE INDEX idx_habit_logs_public_id ON habit_logs(public_id);

-- 4. KAIZEN_DAILY_REFLECTIONS (Kaizen PDCA Günlük Retrospektif Tablosu)
CREATE TABLE IF NOT EXISTS kaizen_daily_reflections (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reflection_date DATE NOT NULL,
    score_percent NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    what_improved_one_percent TEXT,                 -- "Bugün neyi %1 daha iyi yaptım?"
    muda_detected TEXT,                             -- "Fark edilen israf (Muda)"
    pdca_action_for_tomorrow TEXT,                  -- "Yarın için Kaizen eylem planı"
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL,
    CONSTRAINT uq_kaizen_reflection_daily UNIQUE (user_id, reflection_date)
);

CREATE INDEX idx_kaizen_reflections_user ON kaizen_daily_reflections(user_id, reflection_date);
CREATE INDEX idx_kaizen_reflections_public_id ON kaizen_daily_reflections(public_id);


-- 5. MUDA_ITEMS (Muda / Muri / Mura İsraf Takip Tablosu)
CREATE TABLE IF NOT EXISTS muda_items (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    muda_type VARCHAR(20) NOT NULL DEFAULT 'MUDA',  -- 'MUDA' (İsraf), 'MURI' (Aşırı Yük), 'MURA' (Dengesizlik)
    title VARCHAR(255) NOT NULL,
    waste_description TEXT NOT NULL,
    kaizen_countermeasure TEXT NOT NULL,
    eliminated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    last_modified_by VARCHAR(100) DEFAULT 'SYSTEM',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ DEFAULT NULL,
    deleted_by VARCHAR(100) DEFAULT NULL
);

CREATE INDEX idx_muda_user ON muda_items(user_id) WHERE deleted = false;
CREATE INDEX idx_muda_public_id ON muda_items(public_id);
