package com.enterprise.coreapi.domain.habit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record KaizenReflectionResponse(
        UUID publicId,
        LocalDate reflectionDate,
        BigDecimal scorePercent,
        String whatImprovedOnePercent,
        String mudaDetected,
        String pdcaActionForTomorrow
) {}
