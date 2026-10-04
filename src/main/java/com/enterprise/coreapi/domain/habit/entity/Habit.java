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

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "habits")
@SQLDelete(sql = "UPDATE habits SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Habit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "identity_id")
    private Identity identity;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HabitCategory category = HabitCategory.KARIYER;

    @Column(name = "cue_trigger", nullable = false, length = 255)
    private String cueTrigger;

    @Column(name = "target_location", length = 100)
    private String targetLocation = "Çalışma Alanı";

    @Column(name = "habit_stack_current", length = 255)
    private String habitStackCurrent;

    @Column(name = "habit_stack_new", length = 255)
    private String habitStackNew;

    @Column(name = "craving_benefit", length = 255)
    private String cravingBenefit;

    @Column(name = "response_micro_step", nullable = false, length = 255)
    private String responseMicroStep;

    @Column(name = "reward_xp", nullable = false)
    private int rewardXp = 20;

    @Column(nullable = false, length = 30)
    private String frequency = "DAILY";

    @Column(name = "target_minutes")
    private int targetMinutes = 15;

    @Column(name = "current_streak", nullable = false)
    private int currentStreak = 0;

    @Column(name = "best_streak", nullable = false)
    private int bestStreak = 0;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HabitLog> logs = new ArrayList<>();

    public Habit(User user, Identity identity, String title, HabitCategory category,
                 String cueTrigger, String responseMicroStep, int rewardXp) {
        this.user = user;
        this.identity = identity;
        this.title = title;
        this.category = category != null ? category : HabitCategory.KARIYER;
        this.cueTrigger = cueTrigger;
        this.responseMicroStep = responseMicroStep;
        this.rewardXp = rewardXp > 0 ? rewardXp : 20;
    }

    public void incrementStreak() {
        this.currentStreak++;
        if (this.currentStreak > this.bestStreak) {
            this.bestStreak = this.currentStreak;
        }
    }

    public void resetStreak() {
        this.currentStreak = 0;
    }
}
