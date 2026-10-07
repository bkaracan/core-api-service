package com.enterprise.coreapi.domain.habit.dto;

import jakarta.validation.constraints.Size;

public record UpdateHabitRequest(
        String identityPublicId,

        @Size(max = 255, message = "Alışkanlık başlığı en fazla 255 karakter olabilir.")
        String title,

        String category,

        @Size(max = 255, message = "İşaret en fazla 255 karakter olabilir.")
        String cueTrigger,

        @Size(max = 100, message = "Mekan en fazla 100 karakter olabilir.")
        String targetLocation,

        String habitStackCurrent,
        String habitStackNew,
        String cravingBenefit,

        @Size(max = 255, message = "Mikro adım en fazla 255 karakter olabilir.")
        String responseMicroStep,

        Integer rewardXp,
        Integer targetMinutes
) {}
