package com.enterprise.coreapi.common.security;

import com.enterprise.coreapi.common.exception.ErrorCode;
import com.enterprise.coreapi.common.filter.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

/**
 * 403 Forbidden durumlarında Spring Security filtre zincirinde RFC 7807 yanıtı üretir.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        if (traceId == null) {
            traceId = "DENIED-" + System.currentTimeMillis();
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "You do not possess the required permissions to access this resource"
        );
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://api.enterprise.com/errors/forbidden"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", ErrorCode.ACCESS_DENIED.getCode());
        problem.setProperty("traceId", traceId);
        problem.setProperty("timestamp", Instant.now());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
