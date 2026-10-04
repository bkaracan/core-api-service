package com.enterprise.coreapi.domain.habit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateHabitRequest(
        UUID identityPublicId,

        @NotBlank(message = "Alışkanlık başlığı boş bırakılamaz.")
        @Size(max = 255, message = "Alışkanlık başlığı en fazla 255 karakter olabilir.")
        String title,

        String category,

        @NotBlank(message = "1. Yasa: İşaret (Cue) alanı boş bırakılamaz.")
        @Size(max = 255, message = "İşaret en fazla 255 karakter olabilir.")
        String cueTrigger,

        @Size(max = 100, message = "Mekan en fazla 100 karakter olabilir.")
        String targetLocation,

        String habitStackCurrent,
        String habitStackNew,
        String cravingBenefit,

        @NotBlank(message = "3. Yasa: 2-Dakika Kuralı mikro adımı boş bırakılamaz.")
        @Size(max = 255, message = "Mikro adım en fazla 255 karakter olabilir.")
        String responseMicroStep,

        int rewardXp,
        int targetMinutes
) {}
