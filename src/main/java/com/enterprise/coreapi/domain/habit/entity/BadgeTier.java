package com.enterprise.coreapi.domain.habit.entity;

public enum BadgeTier {
    BRONZE("Bronz Küme", "🥉", 10),
    SILVER("Gümüş Küme", "🥈", 25),
    GOLD("Altın Küme", "🥇", 50),
    PLATINUM("Platin Küme", "💠", 100),
    DIAMOND("Elmas Küme", "💎", 0);

    private final String displayName;
    private final String icon;
    private final int badgesRequiredForNext;

    BadgeTier(String displayName, String icon, int badgesRequiredForNext) {
        this.displayName = displayName;
        this.icon = icon;
        this.badgesRequiredForNext = badgesRequiredForNext;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public int getBadgesRequiredForNext() {
        return badgesRequiredForNext;
    }

    /**
     * Toplam kazanılan rozet sayısına göre geçerli kümeyi hesaplar.
     * Bronz: 0-9 (10 rozette Gümüş'e terfi)
     * Gümüş: 10-34 (+25 rozette Altın'a terfi)
     * Altın: 35-84 (+50 rozette Platin'e terfi)
     * Platin: 85-184 (+100 rozette Elmas'a terfi)
     * Elmas: 185+ (Zirve Usta Seviyesi)
     */
    public static BadgeTier fromTotalBadges(int totalBadges) {
        if (totalBadges < 10) return BRONZE;
        if (totalBadges < 35) return SILVER;
        if (totalBadges < 85) return GOLD;
        if (totalBadges < 185) return PLATINUM;
        return DIAMOND;
    }

    /**
     * Kullanıcının bulunduğu mevcut kümede o ana kadar topladığı rozet sayısı.
     */
    public static int getBadgesInCurrentTier(int totalBadges) {
        if (totalBadges < 10) return Math.max(0, totalBadges);
        if (totalBadges < 35) return totalBadges - 10;
        if (totalBadges < 85) return totalBadges - 35;
        if (totalBadges < 185) return totalBadges - 85;
        return totalBadges - 185;
    }

    /**
     * Bir sonraki hedef kümeyi döner.
     */
    public static BadgeTier getNextTier(BadgeTier currentTier) {
        return switch (currentTier) {
            case BRONZE -> SILVER;
            case SILVER -> GOLD;
            case GOLD -> PLATINUM;
            case PLATINUM -> DIAMOND;
            case DIAMOND -> DIAMOND;
        };
    }
}
