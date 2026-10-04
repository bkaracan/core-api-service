-- ============================================================================
-- V5: Generalize Professional Identity
-- Update 'Yazılım Mimarı & Problem Çözücü' to universal 'Üretken Profesyonel & Değer Üreten'
-- ============================================================================

UPDATE identities
SET name = 'Üretken Profesyonel & Değer Üreten',
    tagline = 'Yaptığı işe özen gösterir, odaklanır ve her gün somut değer katar',
    icon = '💼'
WHERE name = 'Yazılım Mimarı & Problem Çözücü';
