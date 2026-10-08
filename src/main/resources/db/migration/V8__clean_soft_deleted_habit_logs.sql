-- =======================================================================
-- Flyway Migration: V8__clean_soft_deleted_habit_logs.sql
-- Temizlik: HabitLog varlığında Hard Delete mimarisine geçiş.
-- Geçmişte soft-delete olarak işaretlenmiş (deleted = true) çöp satırları temizler.
-- =======================================================================

DELETE FROM habit_logs WHERE deleted = true;
