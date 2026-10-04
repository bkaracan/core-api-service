package com.enterprise.coreapi.domain.habit.dto;

public record CategoryProgressResponse(
        String category,
        String categoryDisplayName,
        String icon,
        String currentTier,
        String currentTierName,
        String currentTierIcon,
        int currentTierBadgeCount,
        int nextTierRequiredCount,
        int totalBadgesEarned,
        int progressPercentage,
        String nextTierName,
        boolean maxTierReached
) {}
