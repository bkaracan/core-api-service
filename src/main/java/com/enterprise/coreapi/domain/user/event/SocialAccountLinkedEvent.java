package com.enterprise.coreapi.domain.user.event;

import java.time.Instant;
import java.util.UUID;

public record SocialAccountLinkedEvent(
        UUID userPublicId,
        String provider,
        String providerEmail,
        Instant occurredAt
) {
    public SocialAccountLinkedEvent(UUID userPublicId, String provider, String providerEmail) {
        this(userPublicId, provider, providerEmail, Instant.now());
    }
}
