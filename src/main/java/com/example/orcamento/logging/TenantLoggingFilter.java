package com.example.orcamento.logging;

import com.example.orcamento.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(TenantLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = TenantContext.getTenantId();
        try {
            MDC.put("tenantId", tenantId);
            logger.info("[TENANT] Nova requisição recebida | tenantId={} | método={} | endpoint={}",
                    tenantId, request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("tenantId");
        }
    }
}
