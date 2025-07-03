package com.example.orcamento;

import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.security.TenantContext;
import com.example.orcamento.service.LancamentoCartaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class LancamentoCartaoServiceMultiTenantTest {
    @Mock
    private LancamentoCartaoRepository lancamentoCartaoRepository;

    @InjectMocks
    private LancamentoCartaoService lancamentoCartaoService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
        TenantContext.clear();
    }

    @Test
    void deveRetornarSomenteLancamentosDoTenantAtual() {
        // Simula tenant A
        String tenantA = "tenantA";
        TenantContext.setTenantId(tenantA);

        // Simula retorno do repositório apenas para tenantA
        LancamentoCartao lancamentoA = new LancamentoCartao();
        lancamentoA.setId(1L);
        lancamentoA.setTenantId(tenantA);
        when(lancamentoCartaoRepository.findAll(ArgumentMatchers.<Specification<LancamentoCartao>>any()))
            .thenReturn(List.of(lancamentoA));

        Map<String, Object> filtros = new HashMap<>();
        List<LancamentoCartao> resultadoA = lancamentoCartaoService.listarLancamentosPorFiltrosDinamicos(filtros);
        assertThat(resultadoA).hasSize(1);
        assertThat(resultadoA.get(0).getTenantId()).isEqualTo(tenantA);

        // Simula tenant B
        String tenantB = "tenantB";
        TenantContext.setTenantId(tenantB);
        when(lancamentoCartaoRepository.findAll(ArgumentMatchers.<Specification<LancamentoCartao>>any()))
            .thenReturn(Collections.emptyList());

        List<LancamentoCartao> resultadoB = lancamentoCartaoService.listarLancamentosPorFiltrosDinamicos(filtros);
        assertThat(resultadoB).isEmpty();
    }
}
