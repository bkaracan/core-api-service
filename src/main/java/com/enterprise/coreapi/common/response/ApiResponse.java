package com.enterprise.coreapi.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Kurumsal standart tekil REST API yanıt zarfı.
 *
 * @param <T> Yanıt verisinin nesne tipi
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final @Nullable T data;
    private final @Nullable String traceId;

    @Builder.Default
    private final Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> success(@Nullable T data, @Nullable String traceId) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation completed successfully")
                .data(data)
                .traceId(traceId)
                .build();
    }

    public static <T> ApiResponse<T> success(@Nullable T data, String message, @Nullable String traceId) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .traceId(traceId)
                .build();
    }

    public static <T> ApiResponse<T> empty(@Nullable String traceId) {
        return success(null, traceId);
    }
}
