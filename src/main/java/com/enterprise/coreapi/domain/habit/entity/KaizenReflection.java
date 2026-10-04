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

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "kaizen_daily_reflections",
        uniqueConstraints = @UniqueConstraint(name = "uq_kaizen_reflection_daily", columnNames = {"user_id", "reflection_date"})
)
@SQLDelete(sql = "UPDATE kaizen_daily_reflections SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KaizenReflection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reflection_date", nullable = false)
    private LocalDate reflectionDate;

    @Column(name = "score_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal scorePercent = BigDecimal.ZERO;

    @Column(name = "what_improved_one_percent", columnDefinition = "TEXT")
    private String whatImprovedOnePercent;

    @Column(name = "muda_detected", columnDefinition = "TEXT")
    private String mudaDetected;

    @Column(name = "pdca_action_for_tomorrow", columnDefinition = "TEXT")
    private String pdcaActionForTomorrow;

    public KaizenReflection(User user, LocalDate reflectionDate, BigDecimal scorePercent,
                            String whatImprovedOnePercent, String mudaDetected, String pdcaActionForTomorrow) {
        this.user = user;
        this.reflectionDate = reflectionDate;
        this.scorePercent = scorePercent != null ? scorePercent : BigDecimal.ZERO;
        this.whatImprovedOnePercent = whatImprovedOnePercent;
        this.mudaDetected = mudaDetected;
        this.pdcaActionForTomorrow = pdcaActionForTomorrow;
    }
}
