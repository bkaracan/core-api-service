package com.enterprise.coreapi.domain.user.event;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID userPublicId,
        String email,
        String registrationType,
        Instant occurredAt
) {
    public UserRegisteredEvent(UUID userPublicId, String email, String registrationType) {
        this(userPublicId, email, registrationType, Instant.now());
    }
}
