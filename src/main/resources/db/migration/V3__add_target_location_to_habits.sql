-- =======================================================================
-- Flyway Migration: V3__add_target_location_to_habits.sql
-- Atomic Habits: 1. Yasa - Çevre Tasarımı & Uygulama Niyeti (Mekan / Ortam)
-- Formül: "Şu [ZAMAN]'da, şu [MEKAN]'da, şu [DAVRANIŞ]'ı yapacağım."
-- =======================================================================

ALTER TABLE habits
    ADD COLUMN IF NOT EXISTS target_location VARCHAR(100) DEFAULT 'Çalışma Alanı';
