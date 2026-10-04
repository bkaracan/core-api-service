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

@Entity
@Table(name = "muda_items")
@SQLDelete(sql = "UPDATE muda_items SET deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MudaItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "muda_type", nullable = false, length = 20)
    private String mudaType = "MUDA"; // MUDA (İsraf), MURI (Aşırı Yük), MURA (Dengesizlik)

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "waste_description", nullable = false, columnDefinition = "TEXT")
    private String wasteDescription;

    @Column(name = "kaizen_countermeasure", nullable = false, columnDefinition = "TEXT")
    private String kaizenCountermeasure;

    @Column(nullable = false)
    private boolean eliminated = false;

    public MudaItem(User user, String mudaType, String title, String wasteDescription, String kaizenCountermeasure) {
        this.user = user;
        this.mudaType = mudaType != null ? mudaType : "MUDA";
        this.title = title;
        this.wasteDescription = wasteDescription;
        this.kaizenCountermeasure = kaizenCountermeasure;
        this.eliminated = false;
    }
}
