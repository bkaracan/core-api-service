package com.enterprise.coreapi.common.openapi;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.ProblemDetail;

import java.lang.annotation.*;

/**
 * 200 OK + 404 Not Found + Ortak RFC 7807 Hata Yanıtlarını tek satırda bağlar.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiCommonResponses
@ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Kayıt başarıyla getirildi"
        ),
        @ApiResponse(
                responseCode = "404",
                description = "İstenen kaynak bulunamadı (RFC 7807)",
                content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))
        )
})
public @interface ApiGetResponse {
    @AliasFor(annotation = ApiResponse.class, attribute = "description")
    String description() default "Kayıt başarıyla getirildi";
}
