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
@Table(name = "identities")
@SQLDelete(sql = "UPDATE identities SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Identity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String tagline;

    @Column(nullable = false, length = 20)
    private String icon = "🎯";

    @Column(nullable = false, length = 30)
    private String color = "#6366F1";

    @Column(nullable = false)
    private int level = 1;

    @Column(name = "total_votes", nullable = false)
    private int totalVotes = 0;

    @Column(name = "votes_threshold", nullable = false)
    private int votesThreshold = 50;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @OneToMany(mappedBy = "identity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Habit> habits = new ArrayList<>();

    public Identity(User user, String name, String tagline, String icon, String color, int votesThreshold) {
        this.user = user;
        this.name = name;
        this.tagline = tagline;
        this.icon = icon != null ? icon : "🎯";
        this.color = color != null ? color : "#6366F1";
        this.votesThreshold = votesThreshold > 0 ? votesThreshold : 50;
    }

    public void addVote() {
        this.totalVotes++;
        if (this.totalVotes >= this.votesThreshold) {
            this.level++;
            this.votesThreshold = (int) (this.votesThreshold * 1.5);
        }
    }

    public void removeVote() {
        if (this.totalVotes > 0) {
            this.totalVotes--;
        }
    }
}
