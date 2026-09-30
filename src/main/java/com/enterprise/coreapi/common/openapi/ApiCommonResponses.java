package com.enterprise.coreapi.common.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.*;

/**
 * Tüm endpoint'lerde ortak olan 400, 401, 403 ve 500 RFC 7807 hata şemalarını tek seferde ekler.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Bad Request / Validasyon Hatası (RFC 7807)",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "401",
                description = "Unauthorized / Kimlik Doğrulama Başarısız (RFC 7807)",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Forbidden / Yetkisiz Erişim (RFC 7807)",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal Server Error / Beklenmeyen Sunucu Hatası (RFC 7807)",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
})
public @interface ApiCommonResponses {
}
