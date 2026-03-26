package com.example.orcamento.service;

import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespesaServiceMultiTenantTest {

    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private ContaCorrenteService contaCorrenteService;
    @Mock
    private MovimentacaoService movimentacaoService;
    @Mock
    private MovimentacaoRepository movimentacaoRepository;
    @Mock
    private MetaEconomiaService metaEconomiaService;
    @Mock
    private LancamentoCartaoRepository lancamentoCartaoRepository;
    @Mock
    private ConfiguracaoService configuracaoService;
    @Mock
    private SubcategoriaDespesaRepository subcategoriaDespesaRepository;
    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private DespesaService despesaService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void atualizarPagamentoDeveFalharQuandoDespesaNaoPertenceAoTenantAtual() {
        TenantContext.setTenantId("tenantA");
        when(despesaRepository.findByIdAndTenantId(10L, "tenantA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despesaService.atualizarPagamento(10L, null, null, null, null, null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("tenant atual");
    }

    @Test
    void estornarPagamentoDeveBuscarDespesaComEscopoDoTenant() {
        TenantContext.setTenantId("tenantA");
        when(despesaRepository.findByIdAndTenantId(20L, "tenantA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despesaService.estornarPagamento(20L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(despesaRepository).findByIdAndTenantId(20L, "tenantA");
    }
}
