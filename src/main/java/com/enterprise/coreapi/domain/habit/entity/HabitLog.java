package com.enterprise.coreapi.domain.habit.entity;

import com.enterprise.coreapi.common.entity.BaseEntity;
import com.enterprise.coreapi.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
/**
 * Alışkanlık Günlük Tamamlama Çetelesi (HabitLog).
 * Günlük tamamlama durumu bir transactional state olduğundan ve geri alma (uncheck)
 * işlemi bir arşivleme değil durum iptali olduğundan, veritabanında ölü satır birikmesini
 * ve tekillik çakışmasını (uq_habit_log_daily) önlemek adına HARD DELETE uygulanır.
 */
@Entity
@Table(
        name = "habit_logs",
        uniqueConstraints = @UniqueConstraint(name = "uq_habit_log_daily", columnNames = {"habit_id", "log_date"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HabitLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(nullable = false)
    private boolean completed = true;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt = Instant.now();

    @Column(name = "used_two_minute_rule", nullable = false)
    private boolean usedTwoMinuteRule = false;

    @Column(name = "earned_xp", nullable = false)
    private int earnedXp = 20;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public HabitLog(Habit habit, User user, LocalDate logDate, boolean usedTwoMinuteRule, int earnedXp) {
        this.habit = habit;
        this.user = user;
        this.logDate = logDate;
        this.completed = true;
        this.completedAt = Instant.now();
        this.usedTwoMinuteRule = usedTwoMinuteRule;
        this.earnedXp = earnedXp;
    }
}
