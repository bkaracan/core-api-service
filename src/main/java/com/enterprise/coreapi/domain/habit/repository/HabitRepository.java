package com.enterprise.coreapi.domain.habit.repository;

import com.enterprise.coreapi.domain.habit.entity.Habit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {

    @EntityGraph(attributePaths = {"identity"})
    List<Habit> findAllByUser_PublicIdAndActiveTrueOrderByCreatedAtAsc(UUID userPublicId);

    @EntityGraph(attributePaths = {"identity", "user"})
    Optional<Habit> findByPublicIdAndUser_PublicId(UUID publicId, UUID userPublicId);

    Optional<Habit> findByPublicId(UUID publicId);

    long countByUser_PublicIdAndActiveTrue(UUID userPublicId);
}
