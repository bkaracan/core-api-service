package com.enterprise.coreapi.common.exception;

import org.jspecify.annotations.Nullable;

/**
 * Alan bazlı validasyon hatası detayı.
 */
public record ValidationError(
        String field,
        @Nullable Object rejectedValue,
        String message
) {}
