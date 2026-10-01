package com.enterprise.coreapi.domain.user.dto;

import java.time.Instant;
import java.util.UUID;

public record SocialAccountResponse(
        UUID publicId,
        String provider,
        String providerEmail,
        Instant createdAt
) {}
