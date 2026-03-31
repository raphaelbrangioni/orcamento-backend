package com.example.orcamento.service;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.StatusCartao;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private DespesaRepository despesaRepository;
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
        when(despesaRepository.findByDataVencimentoBetweenAndDataPagamentoIsNull("tenantA", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of());
        when(despesaService.verificarFaturaLancada("Visa Infinite", 3, 2026)).thenReturn(true);

        DashboardHomeDTO dashboard = dashboardHomeService.obterDashboard(2026, 3);

        assertThat(dashboard.getCartoes()).hasSize(1);
        assertThat(dashboard.getCartoes().get(0).getDiaVencimento()).isEqualTo(15);
        assertThat(dashboard.getCartoes().get(0).isFaturaLancada()).isTrue();
        assertThat(dashboard.getCartoes().get(0).getValorFatura()).isEqualByComparingTo("1200.00");
        assertThat(dashboard.getTotalFaturasCartoes()).isEqualByComparingTo("1200.00");
    }

    @Test
    void obterDashboardDeveRetornarAlertasDeFaturasDespesasVencidasEContasNegativas() {
        TenantContext.setTenantId("tenantA");
        YearMonth competenciaAtual = YearMonth.from(LocalDate.now());

        when(fechamentoMensalService.obterResumoMensal(competenciaAtual.getYear(), competenciaAtual.getMonthValue()))
                .thenReturn(FechamentoMensalResponseDTO.builder()
                        .ano(competenciaAtual.getYear())
                        .mes(competenciaAtual.getMonthValue())
                        .totalFaturasNaoLancadas(new BigDecimal("1611.63"))
                        .build());
        when(contaCorrenteService.listarTodos()).thenReturn(List.of(
                new ContaCorrente(1L, "0001", "123", "001", "Banco X", new BigDecimal("-250.00"), 1L, "tenantA", true),
                new ContaCorrente(2L, "0001", "124", "001", "Banco Y", new BigDecimal("100.00"), 1L, "tenantA", true)
        ));
        CartaoCredito cartaoNaoLancado = new CartaoCredito();
        cartaoNaoLancado.setId(10L);
        cartaoNaoLancado.setNome("Visa Infinite");
        cartaoNaoLancado.setDiaVencimento(LocalDate.now().getDayOfMonth());
        cartaoNaoLancado.setStatus(StatusCartao.ATIVO);
        cartaoNaoLancado.setTenantId("tenantA");
        when(cartaoCreditoRepository.findByTenantId("tenantA")).thenReturn(List.of(cartaoNaoLancado));
        String mesAnoFatura = competenciaAtual.getMonth()
                .getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("pt", "BR"))
                .toUpperCase(java.util.Locale.ROOT)
                .replace('Ç', 'C')
                .replace('Ã', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U') + "/" + competenciaAtual.getYear();
        when(lancamentoCartaoRepository.getFaturaDoMes(10L, mesAnoFatura, "tenantA")).thenReturn(new BigDecimal("1611.63"));
        when(lancamentoCartaoRepository.getFaturaDoMesTerceiros(10L, mesAnoFatura, "tenantA")).thenReturn(BigDecimal.ZERO);
        when(despesaService.verificarFaturaLancada("Visa Infinite", competenciaAtual.getMonthValue(), competenciaAtual.getYear())).thenReturn(false);

        Despesa vencida = Despesa.builder()
                .id(1L)
                .tenantId("tenantA")
                .nome("Conta de luz")
                .valorPrevisto(new BigDecimal("320.00"))
                .dataVencimento(LocalDate.of(2026, 3, 10))
                .build();
        when(despesaRepository.findByDataVencimentoBetweenAndDataPagamentoIsNull("tenantA", competenciaAtual.atDay(1), LocalDate.now()))
                .thenReturn(List.of(vencida));

        DashboardHomeDTO dashboard = dashboardHomeService.obterDashboard(competenciaAtual.getYear(), competenciaAtual.getMonthValue());

        assertThat(dashboard.getAlertas()).hasSize(4);
        assertThat(dashboard.getAlertas()).extracting("tipo")
                .containsExactlyInAnyOrder("FATURAS_NAO_LANCADAS", "FATURAS_A_VENCER", "DESPESAS_VENCIDAS_NAO_PAGAS", "CONTAS_NEGATIVAS");
        assertThat(dashboard.getAlertas())
                .filteredOn(alerta -> "FATURAS_NAO_LANCADAS".equals(alerta.getTipo()))
                .first()
                .extracting("quantidade", "valor")
                .containsExactly(1, new BigDecimal("1611.63"));
        assertThat(dashboard.getAlertas())
                .filteredOn(alerta -> "FATURAS_A_VENCER".equals(alerta.getTipo()))
                .first()
                .extracting("quantidade", "valor")
                .containsExactly(1, new BigDecimal("1611.63"));
        assertThat(dashboard.getAlertas())
                .filteredOn(alerta -> "DESPESAS_VENCIDAS_NAO_PAGAS".equals(alerta.getTipo()))
                .first()
                .extracting("quantidade", "valor")
                .containsExactly(1, new BigDecimal("320.00"));
        assertThat(dashboard.getAlertas())
                .filteredOn(alerta -> "CONTAS_NEGATIVAS".equals(alerta.getTipo()))
                .first()
                .extracting("quantidade", "valor")
                .containsExactly(1, new BigDecimal("250.00"));
    }
}
