package com.enterprise.coreapi.domain.user.event;

import java.time.Instant;
import java.util.UUID;

public record UserPasswordUpdatedEvent(
        UUID userPublicId,
        String email,
        Instant occurredAt
) {
    public UserPasswordUpdatedEvent(UUID userPublicId, String email) {
        this(userPublicId, email, Instant.now());
    }
}
