package com.enterprise.coreapi.common.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.*;

/**
 * 204 No Content + 404 Not Found + Ortak RFC 7807 Hataları.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiCommonResponses
@ApiResponses({
        @ApiResponse(
                responseCode = "204",
                description = "Kaynak başarıyla silindi (İçerik dönmez)",
                content = @Content
        ),
        @ApiResponse(
                responseCode = "404",
                description = "Silinmek istenen kaynak bulunamadı (RFC 7807)",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
})
public @interface ApiDeleteResponse {
}
