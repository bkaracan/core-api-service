-- V4__cleanup_seeded_default_habits.sql
-- Daha önce otomatik tohumlanan (seed) varsayılan mock alışkanlık kayıtlarının temizlenmesi
-- Kullanıcıya sıfır varsayılan görev dayatması ve tam özgürlük sunulması amacıyla

DELETE FROM habit_logs 
WHERE habit_id IN (
    SELECT id FROM habits 
    WHERE title IN (
        '25 Dakika Derin Odaklı Kod Geliştirme (Deep Work)',
        'Günde 2 Litre Su Tüketimi & Hidrasyon',
        '15 Sayfa Mimari veya Felsefi Kitap Okuma',
        '15 Dakika Tempolu Yürüyüş & Postür Düzeltme',
        'Günün Kaizen Değerlendirmesi: "Bugün neyi %1 iyileştirdim?"'
    )
);

DELETE FROM habits 
WHERE title IN (
    '25 Dakika Derin Odaklı Kod Geliştirme (Deep Work)',
    'Günde 2 Litre Su Tüketimi & Hidrasyon',
    '15 Sayfa Mimari veya Felsefi Kitap Okuma',
    '15 Dakika Tempolu Yürüyüş & Postür Düzeltme',
    'Günün Kaizen Değerlendirmesi: "Bugün neyi %1 iyileştirdim?"'
);
