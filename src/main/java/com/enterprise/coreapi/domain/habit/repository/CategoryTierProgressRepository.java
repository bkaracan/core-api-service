package com.enterprise.coreapi.domain.habit.repository;

import com.enterprise.coreapi.domain.habit.entity.CategoryTierProgress;
import com.enterprise.coreapi.domain.habit.entity.HabitCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryTierProgressRepository extends JpaRepository<CategoryTierProgress, Long> {

    Optional<CategoryTierProgress> findByUser_PublicIdAndCategory(UUID userPublicId, HabitCategory category);

    List<CategoryTierProgress> findAllByUser_PublicId(UUID userPublicId);
}
