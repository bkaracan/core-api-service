package com.enterprise.coreapi.domain.habit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIdentityRequest(
        @NotBlank(message = "Kimlik adı boş bırakılamaz.")
        @Size(max = 100, message = "Kimlik adı en fazla 100 karakter olabilir.")
        String name,

        @NotBlank(message = "Kimlik sloganı/tanımı boş bırakılamaz.")
        @Size(max = 255, message = "Kimlik sloganı en fazla 255 karakter olabilir.")
        String tagline,

        String icon,
        String color,
        int votesThreshold
) {}
