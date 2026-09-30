package com.enterprise.coreapi.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * En yüksek öncelikle çalışarak her HTTP isteğine benzersiz bir X-Trace-Id atar
 * ve SLF4J MDC havuzuna yazar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Varsa Gateway'den gelen trace ID'yi al, yoksa yeni üret
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }

        // 2. Logback MDC havuzuna koy (tüm log satırlarında görünür)
        MDC.put(MDC_TRACE_ID_KEY, traceId);

        // 3. İstemciye dönen HTTP Response Header'ına ekle
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Thread havuzu kirlenmesini engellemek için thread bitiminde temizle
            MDC.remove(MDC_TRACE_ID_KEY);
        }
    }
}
