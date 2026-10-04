package com.enterprise.coreapi.domain.habit.dto;

public record KaizenReflectionRequest(
        String whatImprovedOnePercent,
        String mudaDetected,
        String pdcaActionForTomorrow
) {}
