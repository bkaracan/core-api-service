package com.enterprise.coreapi.domain.habit.service;

import com.enterprise.coreapi.domain.habit.dto.*;

import java.util.List;
import java.util.UUID;

public interface HabitService {

    List<HabitResponse> getHabitsForToday(UUID userPublicId);

    HabitResponse createHabit(UUID userPublicId, CreateHabitRequest request);

    HabitResponse toggleHabitCompletion(UUID habitPublicId, UUID userPublicId, boolean usedTwoMinuteRule);

    DashboardSummaryResponse getDashboardSummary(UUID userPublicId);

    KaizenReflectionResponse saveDailyReflection(UUID userPublicId, KaizenReflectionRequest request);

    HabitResponse updateHabit(UUID userPublicId, UUID habitPublicId, UpdateHabitRequest request);

    void deleteHabit(UUID userPublicId, UUID habitPublicId);
}
