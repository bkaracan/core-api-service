package com.enterprise.coreapi.domain.user.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserProfileResponse(
        UUID publicId,
        String email,
        String firstName,
        String lastName,
        boolean hasLocalPassword,
        String status,
        Set<String> roles,
        Set<SocialAccountResponse> socialAccounts,
        Instant createdAt
) {}
