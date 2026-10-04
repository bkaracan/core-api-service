package com.enterprise.coreapi.domain.habit.dto;

import java.util.List;

public record DashboardSummaryResponse(
        int currentStreak,
        int totalHabits,
        int completedHabits,
        int completionRate,
        int totalEarnedXp,
        int totalIdentityVotes,
        List<IdentityResponse> identities,
        List<HabitResponse> habits,
        KaizenReflectionResponse todayReflection,
        List<CategoryProgressResponse> categoryTiers
) {}
