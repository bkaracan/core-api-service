package com.enterprise.coreapi.domain.habit.repository;

import com.enterprise.coreapi.domain.habit.entity.KaizenReflection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KaizenReflectionRepository extends JpaRepository<KaizenReflection, Long> {

    Optional<KaizenReflection> findByUser_PublicIdAndReflectionDate(UUID userPublicId, LocalDate reflectionDate);

    List<KaizenReflection> findTop7ByUser_PublicIdOrderByReflectionDateDesc(UUID userPublicId);
}
