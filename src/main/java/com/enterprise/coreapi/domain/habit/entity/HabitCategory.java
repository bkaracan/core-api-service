package com.enterprise.coreapi.domain.habit.entity;

public enum HabitCategory {
    ZIHIN("Zihin / Gelişim"),
    BEDEN("Beden / Sağlık"),
    KARIYER("Mesleki / Çalışma"),
    ODAK("Farkındalık / Meditasyon"),
    SOSYAL("Sosyal / Etkinlik"),
    HOBI("Hobi & Yaratıcılık"),
    SINEMA_KULTUR("Dizi / Film / Kültür"),
    EGLENCE_OYUN("Oyun & Eğlence");

    private final String displayName;

    HabitCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
