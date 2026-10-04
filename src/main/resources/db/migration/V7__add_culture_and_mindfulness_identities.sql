-- =======================================================================
-- Flyway Migration: V7__add_culture_and_mindfulness_identities.sql
-- Add dedicated identities for 'SINEMA_KULTUR' (Kültür Sanat & Sinema Tutkunu)
-- and 'ODAK' (Bilinçli ve Dingin Zihin)
-- =======================================================================

-- 1. SINEMA_KULTUR için Kültür Sanat & Sinema Tutkunu kimliğini ekle
INSERT INTO identities (
    public_id, version, user_id, name, tagline, icon, color, level, total_votes, votes_threshold, display_order, created_at, updated_at
)
SELECT 
    gen_random_uuid(), 0, u.id,
    'Kültür Sanat & Sinema Tutkunu',
    'Sinema, tiyatro ve sanatla vizyonunu genişletir; hikayelerden ilham alır',
    '🎬',
    '#F43F5E',
    1, 0, 30, 7,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM identities i WHERE i.user_id = u.id AND i.name = 'Kültür Sanat & Sinema Tutkunu'
);

-- 2. ODAK için Bilinçli ve Dingin Zihin kimliğini ekle
INSERT INTO identities (
    public_id, version, user_id, name, tagline, icon, color, level, total_votes, votes_threshold, display_order, created_at, updated_at
)
SELECT 
    gen_random_uuid(), 0, u.id,
    'Bilinçli ve Dingin Zihin',
    'Nefesine ve ana odaklanır, zihinsel dinginlik ve berraklık kazanır',
    '🧘',
    '#14B8A6',
    1, 0, 30, 8,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM identities i WHERE i.user_id = u.id AND i.name = 'Bilinçli ve Dingin Zihin'
);
