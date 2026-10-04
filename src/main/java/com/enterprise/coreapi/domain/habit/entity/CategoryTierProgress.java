package com.enterprise.coreapi.domain.habit.entity;

import com.enterprise.coreapi.common.entity.BaseEntity;
import com.enterprise.coreapi.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * 4. Yasa Doyurucu Kıl: Kategori Rozetleri & Küme Terfi Sistemi Varlığı.
 * Kullanıcının her bir kategoride kazandığı rozetleri ve küme ilerlemesini tutar.
 */
@Entity
@Table(
        name = "category_tier_progress",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_category_tier_progress_user_cat", columnNames = {"user_id", "category"})
        }
)
@SQLDelete(sql = "UPDATE category_tier_progress SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryTierProgress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HabitCategory category;

    @Column(name = "total_badges", nullable = false)
    private int totalBadges = 0;

    public CategoryTierProgress(User user, HabitCategory category) {
        this.user = user;
        this.category = category;
        this.totalBadges = 0;
    }

    public void incrementBadge() {
        this.totalBadges++;
    }

    public void decrementBadge() {
        if (this.totalBadges > 0) {
            this.totalBadges--;
        }
    }
}
