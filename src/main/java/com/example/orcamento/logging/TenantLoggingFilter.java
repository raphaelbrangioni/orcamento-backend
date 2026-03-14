package com.example.orcamento.logging;

import com.example.orcamento.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.Arrays;

@Component
@Order(1) // Garante que este filtro seja executado primeiro
public class TenantLoggingFilter extends OncePerRequestFilter implements InitializingBean {
    
    private static final Logger logger = LoggerFactory.getLogger(TenantLoggingFilter.class);
    
    @Override
    public void afterPropertiesSet() {
        logger.info("Inicializando TenantLoggingFilter...");
        
        try {
            logger.info("Tentando carregar a classe TenantContext...");
            
            // Verifica se a classe TenantContext pode ser carregada
            Class<?> tenantContextClass = Class.forName("com.example.orcamento.security.TenantContext");
            logger.info("✅ TenantContext carregado com sucesso: {}", tenantContextClass);
            logger.info("🔍 ClassLoader do TenantContext: {}", tenantContextClass.getClassLoader());
            logger.info("🔍 ClassLoader do TenantLoggingFilter: {}", this.getClass().getClassLoader());
            
            // Verifica se os métodos necessários existem
            logger.info("🔍 Verificando métodos de TenantContext...");
            Arrays.stream(tenantContextClass.getMethods())
                .filter(m -> m.getName().equals("getTenantId") || m.getName().equals("setTenantId"))
                .forEach(m -> logger.info("✅ Método encontrado: {}", m));
                
            // Tenta usar a classe diretamente
            String initialTenantId = TenantContext.getTenantId();
            logger.info("✅ Chamada estática para TenantContext.getTenantId() retornou: {}", 
                initialTenantId != null ? initialTenantId : "null");
                
        } catch (ClassNotFoundException e) {
            String errorMsg = "❌ ERRO CRÍTICO: Não foi possível carregar a classe TenantContext";
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "❌ Erro ao verificar TenantContext";
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startNs = System.nanoTime();
        logger.debug("Iniciando filtro de tenant para: {} {}", request.getMethod(), request.getRequestURI());
        String tenantId = null;
        String tenantIdHeader = request.getHeader("x-tenant-id");
        String traceIdHeader = request.getHeader("x-trace-id");
        if (traceIdHeader == null || traceIdHeader.isBlank()) {
            traceIdHeader = request.getHeader("x-correlation-id");
        }
        String traceId = (traceIdHeader != null && !traceIdHeader.isBlank()) ? traceIdHeader : UUID.randomUUID().toString();
        try {
            tenantId = TenantContext.getTenantId();
            logger.debug("Tenant ID obtido: {}", tenantId);
        } catch (Exception e) {
            logger.error("Erro ao obter Tenant ID", e);
            throw e;
        }
        try {
            String tenantIdLog = (tenantIdHeader != null && !tenantIdHeader.isBlank()) ? tenantIdHeader : tenantId;
            MDC.put("tenantId", tenantIdLog != null ? tenantIdLog : "");
            MDC.put("traceId", traceId);
            logger.info("[TENANT] Nova requisição recebida | tenantId={} | método={} | endpoint={}",
                    tenantIdLog, request.getMethod(), request.getRequestURI());
            try {
                filterChain.doFilter(request, response);
            } catch (Throwable t) {
                logger.error(
                        "[TENANT] Erro durante processamento da requisição | tenantId={} | método={} | endpoint={}",
                        tenantIdLog,
                        request.getMethod(),
                        request.getRequestURI(),
                        t
                );
                throw t;
            } finally {
                long durationMs = (System.nanoTime() - startNs) / 1_000_000;
                logger.info(
                        "[TENANT] Requisição finalizada | tenantId={} | método={} | endpoint={} | status={} | durationMs={}",
                        tenantIdLog,
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        durationMs
                );
            }
        } finally {
            MDC.remove("tenantId");
            MDC.remove("traceId");
        }
    }
}
