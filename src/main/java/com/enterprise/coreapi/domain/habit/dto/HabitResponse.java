package com.enterprise.coreapi.domain.habit.dto;

import java.util.UUID;

public record HabitResponse(
        UUID publicId,
        UUID identityPublicId,
        String identityName,
        String title,
        String category,
        String cueTrigger,
        String targetLocation,
        String habitStackCurrent,
        String habitStackNew,
        String cravingBenefit,
        String responseMicroStep,
        int rewardXp,
        String frequency,
        int targetMinutes,
        int currentStreak,
        int bestStreak,
        boolean active,
        boolean completedToday
) {}
