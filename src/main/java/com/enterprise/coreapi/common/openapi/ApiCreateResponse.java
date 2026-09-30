package com.enterprise.coreapi.common.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.*;

/**
 * 201 Created + 409 Conflict (Duplicate resource) + Ortak RFC 7807 Hata Yanıtları.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiCommonResponses
@ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Kaynak başarıyla oluşturuldu"
        ),
        @ApiResponse(
                responseCode = "409",
                description = "Veritabanı kısıt/çakışma hatası (Duplicate resource) (RFC 7807)",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
})
public @interface ApiCreateResponse {
    @AliasFor(annotation = ApiResponse.class, attribute = "description")
    String description() default "Kaynak başarıyla oluşturuldu";
}
