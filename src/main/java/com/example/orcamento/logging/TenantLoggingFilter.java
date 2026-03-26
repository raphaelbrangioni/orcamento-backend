package com.example.orcamento.logging;

import com.example.orcamento.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class TenantLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TenantLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNs = System.nanoTime();

        String tenantIdHeader = request.getHeader("x-tenant-id");
        String traceIdHeader = request.getHeader("x-trace-id");
        if (traceIdHeader == null || traceIdHeader.isBlank()) {
            traceIdHeader = request.getHeader("x-correlation-id");
        }

        String traceId = (traceIdHeader != null && !traceIdHeader.isBlank())
                ? traceIdHeader
                : UUID.randomUUID().toString();

        String tenantIdContext = TenantContext.getTenantId();
        String tenantId = (tenantIdHeader != null && !tenantIdHeader.isBlank())
                ? tenantIdHeader
                : tenantIdContext;

        try {
            MDC.put("tenantId", tenantId != null ? tenantId : "");
            MDC.put("traceId", traceId);

            logger.info(
                    "request.start tenantId={} method={} path={}",
                    tenantId,
                    request.getMethod(),
                    request.getRequestURI()
            );

            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000;
            logger.info(
                    "request.end tenantId={} method={} path={} status={} durationMs={}",
                    tenantId,
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs
            );
            MDC.remove("tenantId");
            MDC.remove("traceId");
        }
    }
}
