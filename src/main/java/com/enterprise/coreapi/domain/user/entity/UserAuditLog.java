package com.enterprise.coreapi.domain.user.entity;

import com.enterprise.coreapi.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_audit_log")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuditLog extends BaseEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "auth_type", nullable = false, length = 50)
    private String authType;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    public UserAuditLog(Long userId, String authType, String action, String ipAddress, String userAgent, String details) {
        this.userId = userId;
        this.authType = authType;
        this.action = action;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.details = details;
    }
}
