package com.example.orcamento.service;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.StatusCartao;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardHomeServiceTest {

    @Mock
    private FechamentoMensalService fechamentoMensalService;
    @Mock
    private ContaCorrenteService contaCorrenteService;
    @Mock
    private CartaoCreditoRepository cartaoCreditoRepository;
    @Mock
    private LancamentoCartaoRepository lancamentoCartaoRepository;
    @Mock
    private DespesaService despesaService;

    @InjectMocks
    private DashboardHomeService dashboardHomeService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void obterDashboardDeveRetornarDiaDeVencimentoEFaturaLancadaPorCartao() {
        TenantContext.setTenantId("tenantA");

        when(fechamentoMensalService.obterResumoMensal(2026, 3))
                .thenReturn(FechamentoMensalResponseDTO.builder().ano(2026).mes(3).build());
        when(contaCorrenteService.listarTodos()).thenReturn(List.of(
                new ContaCorrente(1L, "0001", "123", "001", "Banco X", new BigDecimal("100.00"), 1L, "tenantA", true)
        ));

        CartaoCredito cartao = new CartaoCredito();
        cartao.setId(10L);
        cartao.setNome("Visa Infinite");
        cartao.setLimite(new BigDecimal("10000.00"));
        cartao.setDiaVencimento(15);
        cartao.setStatus(StatusCartao.ATIVO);
        cartao.setTenantId("tenantA");

        when(cartaoCreditoRepository.findByTenantId("tenantA")).thenReturn(List.of(cartao));
        when(lancamentoCartaoRepository.getFaturaDoMes(10L, "MARCO/2026", "tenantA")).thenReturn(new BigDecimal("1200.00"));
        when(lancamentoCartaoRepository.getFaturaDoMesTerceiros(10L, "MARCO/2026", "tenantA")).thenReturn(new BigDecimal("200.00"));
        when(despesaService.verificarFaturaLancada("Visa Infinite", 3, 2026)).thenReturn(true);

        DashboardHomeDTO dashboard = dashboardHomeService.obterDashboard(2026, 3);

        assertThat(dashboard.getCartoes()).hasSize(1);
        assertThat(dashboard.getCartoes().get(0).getDiaVencimento()).isEqualTo(15);
        assertThat(dashboard.getCartoes().get(0).isFaturaLancada()).isTrue();
        assertThat(dashboard.getCartoes().get(0).getValorFatura()).isEqualByComparingTo("1200.00");
        assertThat(dashboard.getTotalFaturasCartoes()).isEqualByComparingTo("1200.00");
    }
}
