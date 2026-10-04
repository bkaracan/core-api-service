package com.enterprise.coreapi.domain.habit.repository;

import com.enterprise.coreapi.domain.habit.entity.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    List<HabitLog> findAllByUser_PublicIdAndLogDate(UUID userPublicId, LocalDate logDate);

    List<HabitLog> findAllByHabit_PublicId(UUID habitPublicId);

    Optional<HabitLog> findByHabit_PublicIdAndLogDate(UUID habitPublicId, LocalDate logDate);

    boolean existsByHabit_IdAndLogDate(Long habitId, LocalDate logDate);
}
