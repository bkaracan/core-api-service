package com.enterprise.coreapi.domain.user.repository;

import com.enterprise.coreapi.domain.user.entity.UserAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, Long> {

    List<UserAuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);
}
