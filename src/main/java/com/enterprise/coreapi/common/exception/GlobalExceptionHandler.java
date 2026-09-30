package com.enterprise.coreapi.common.exception;

import com.enterprise.coreapi.common.filter.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Spring Boot 4.1.1 kurumsal merkezi istisna yakalayıcısı (RFC 7807 ProblemDetail).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_BASE = "https://api.enterprise.com/errors/";

    /**
     * 1. BEAN VALIDATION (@Valid, @NotNull, @Size vb.) HATALARINI YAKALAR -> HTTP 400
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String path = getPath(request);
        String traceId = getTraceId();

        List<ValidationError> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ValidationError(err.getField(), err.getRejectedValue(), err.getDefaultMessage()))
                .toList();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Input validation failed for one or more fields"
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setType(URI.create(TYPE_BASE + "validation-error"));
        problemDetail.setInstance(URI.create(path));
        problemDetail.setProperty("errorCode", ErrorCode.VALIDATION_FAILED.getCode());
        problemDetail.setProperty("errors", validationErrors);
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("timestamp", Instant.now());

        log.warn("[{}] Validation error at {}: {}", traceId, path, validationErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * 2. ÖZEL İŞ MANTIĞI VE DOMAIN İSTİSNALARI -> Dynamic Status (404, 400 vb.)
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String path = request.getRequestURI();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getHttpStatus(), ex.getMessage());
        problemDetail.setTitle(ex.getErrorCode().name().replace('_', ' '));
        problemDetail.setType(URI.create(TYPE_BASE + ex.getErrorCode().getCode().toLowerCase()));
        problemDetail.setInstance(URI.create(path));
        problemDetail.setProperty("errorCode", ex.getErrorCode().getCode());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("timestamp", Instant.now());

        log.warn("[{}] Business exception at {}: [{}] {}", traceId, path, ex.getErrorCode().getCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(problemDetail);
    }

    /**
     * 3. VERİTABANI KISITLAMA / UNIQUE CONSTRAINT İHLALLERİ -> HTTP 409 CONFLICT
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String path = request.getRequestURI();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Database constraint violation detected (e.g. duplicate key or foreign key conflict)"
        );
        problemDetail.setTitle("Database Conflict");
        problemDetail.setType(URI.create(TYPE_BASE + "data-conflict"));
        problemDetail.setInstance(URI.create(path));
        problemDetail.setProperty("errorCode", ErrorCode.RESOURCE_ALREADY_EXISTS.getCode());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("timestamp", Instant.now());

        log.error("[{}] Data conflict at {}: {}", traceId, path, ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * 4. BEKLENMEYEN TÜM SİSTEM HATALARI (FALLBACK) -> HTTP 500 INTERNAL SERVER ERROR
     * StackTrace ASLA istemciye sızdırılmaz!
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, HttpServletRequest request) {
        String traceId = getTraceId();
        String path = request.getRequestURI();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please provide the traceId to support."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create(TYPE_BASE + "internal-error"));
        problemDetail.setInstance(URI.create(path));
        problemDetail.setProperty("errorCode", ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        problemDetail.setProperty("traceId", traceId);
        problemDetail.setProperty("timestamp", Instant.now());

        // StackTrace SADECE loglara basılır; asla istemciye verilmez!
        log.error("[{}] Unhandled internal exception at {}: ", traceId, path, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    private String getTraceId() {
        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        return traceId != null ? traceId : "N/A";
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return "/";
    }
}
