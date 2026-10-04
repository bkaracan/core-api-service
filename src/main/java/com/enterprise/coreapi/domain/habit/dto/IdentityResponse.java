package com.enterprise.coreapi.domain.habit.dto;

import java.util.UUID;

public record IdentityResponse(
        UUID publicId,
        String name,
        String tagline,
        String icon,
        String color,
        int level,
        int totalVotes,
        int votesThreshold,
        int displayOrder
) {}
