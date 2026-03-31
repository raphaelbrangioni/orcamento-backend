package com.example.orcamento.service;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.FechamentoMensal;
import com.example.orcamento.model.FechamentoMensalHistorico;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.Receita;
import com.example.orcamento.model.enums.FormaDePagamento;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.ContaCorrenteSaldoDiaRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.FechamentoMensalHistoricoRepository;
import com.example.orcamento.repository.FechamentoMensalRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.ReceitaRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FechamentoMensalServiceTest {

    @Mock
    private FechamentoMensalRepository fechamentoMensalRepository;
    @Mock
    private ContaCorrenteService contaCorrenteService;
    @Mock
    private ContaCorrenteSaldoDiaRepository contaCorrenteSaldoDiaRepository;
    @Mock
    private ReceitaRepository receitaRepository;
    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private LancamentoCartaoRepository lancamentoCartaoRepository;
    @Mock
    private CartaoCreditoRepository cartaoCreditoRepository;
    @Mock
    private FechamentoMensalHistoricoRepository fechamentoMensalHistoricoRepository;

    @InjectMocks
    private FechamentoMensalService fechamentoMensalService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void obterResumoMensalDeveManterConsistenciaEntrePagasCaixaECartao() {
        TenantContext.setTenantId("tenantA");

        when(fechamentoMensalRepository.findByTenantIdAndAnoAndMes("tenantA", 2026, 3)).thenReturn(Optional.empty());
        when(fechamentoMensalRepository.findByTenantIdAndAnoAndMes("tenantA", 2026, 2)).thenReturn(Optional.empty());
        when(contaCorrenteService.listarTodos()).thenReturn(List.of(
                new ContaCorrente(1L, "0001", "123", "001", "Banco X", BigDecimal.TEN, 1L, "tenantA", true)
        ));

        Receita receita = Receita.builder()
                .id(1L)
                .tenantId("tenantA")
                .descricao("Salario")
                .valor(new BigDecimal("1000.00"))
                .dataRecebimento(LocalDate.of(2026, 3, 5))
                .isPrevista(false)
                .build();
        when(receitaRepository.findByDataRecebimentoBetweenAndTenantId(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "tenantA")
        ).thenReturn(List.of(receita));

        Despesa despesaDebito = Despesa.builder()
                .id(1L)
                .tenantId("tenantA")
                .nome("Aluguel")
                .valorPrevisto(new BigDecimal("500.00"))
                .valorPago(new BigDecimal("500.00"))
                .dataVencimento(LocalDate.of(2026, 3, 10))
                .formaDePagamento(FormaDePagamento.DEBITO)
                .build();
        Despesa despesaCredito = Despesa.builder()
                .id(2L)
                .tenantId("tenantA")
                .nome("Mercado")
                .valorPrevisto(new BigDecimal("300.00"))
                .valorPago(new BigDecimal("300.00"))
                .dataVencimento(LocalDate.of(2026, 3, 15))
                .formaDePagamento(FormaDePagamento.CREDITO)
                .build();
        when(despesaRepository.findByTenantIdAndDataVencimentoBetween(
                "tenantA",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31))
        ).thenReturn(List.of(despesaDebito, despesaCredito));
        when(lancamentoCartaoRepository.findByMesAnoFaturaAndTenantId("MARCO/2026", "tenantA")).thenReturn(List.of());
        when(cartaoCreditoRepository.findByTenantId("tenantA")).thenReturn(List.of());

        FechamentoMensalResponseDTO resumo = fechamentoMensalService.obterResumoMensal(2026, 3);

        assertThat(resumo.getDespesasPagas()).isEqualByComparingTo("800.00");
        assertThat(resumo.getDespesasPagasNoCaixa()).isEqualByComparingTo("500.00");
        assertThat(resumo.getDespesasPagasCartao()).isEqualByComparingTo("300.00");
        assertThat(resumo.getDespesasPagasNoCaixa().add(resumo.getDespesasPagasCartao()))
                .isEqualByComparingTo(resumo.getDespesasPagas());
        assertThat(resumo.getSaldoFinal()).isEqualByComparingTo("500.00");
    }

    @Test
    void obterResumoMensalDeveExplicarFaturasLancadasENaoLancadas() {
        TenantContext.setTenantId("tenantA");

        when(fechamentoMensalRepository.findByTenantIdAndAnoAndMes("tenantA", 2026, 4)).thenReturn(Optional.empty());
        when(fechamentoMensalRepository.findByTenantIdAndAnoAndMes("tenantA", 2026, 3)).thenReturn(Optional.empty());
        when(contaCorrenteService.listarTodos()).thenReturn(List.of());
        when(receitaRepository.findByDataRecebimentoBetweenAndTenantId(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                "tenantA")
        ).thenReturn(List.of());

        Despesa faturaLancada = Despesa.builder()
                .id(10L)
                .tenantId("tenantA")
                .nome("Fatura Cartão Visa Infinite")
                .valorPrevisto(new BigDecimal("1000.00"))
                .dataVencimento(LocalDate.of(2026, 4, 2))
                .build();
        Despesa despesaNormal = Despesa.builder()
                .id(11L)
                .tenantId("tenantA")
                .nome("Aluguel")
                .valorPrevisto(new BigDecimal("500.00"))
                .dataVencimento(LocalDate.of(2026, 4, 10))
                .build();
        when(despesaRepository.findByTenantIdAndDataVencimentoBetween(
                "tenantA",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30))
        ).thenReturn(List.of(faturaLancada, despesaNormal));

        LancamentoCartao lancamentoVisa = LancamentoCartao.builder()
                .valorTotal(new BigDecimal("1000.00"))
                .proprietario("Proprio")
                .build();
        LancamentoCartao lancamentoInterProprio = LancamentoCartao.builder()
                .valorTotal(new BigDecimal("500.00"))
                .proprietario("Proprio")
                .build();
        LancamentoCartao lancamentoInterTerceiro = LancamentoCartao.builder()
                .valorTotal(new BigDecimal("200.00"))
                .proprietario("Terceiros")
                .build();
        when(lancamentoCartaoRepository.findByMesAnoFaturaAndTenantId("ABRIL/2026", "tenantA"))
                .thenReturn(List.of(lancamentoVisa, lancamentoInterProprio, lancamentoInterTerceiro));

        CartaoCredito visa = new CartaoCredito();
        visa.setId(1L);
        visa.setNome("Visa Infinite");
        CartaoCredito inter = new CartaoCredito();
        inter.setId(2L);
        inter.setNome("Cartão Inter");
        when(cartaoCreditoRepository.findByTenantId("tenantA")).thenReturn(List.of(visa, inter));
        when(despesaRepository.existsByNomeLikeAndMesAndAno("tenantA", "Fatura Cartao Visa Infinite", 4, 2026)).thenReturn(true);
        when(despesaRepository.existsByNomeLikeAndMesAndAno("tenantA", "Fatura Cartao Cartão Inter", 4, 2026)).thenReturn(false);
        when(lancamentoCartaoRepository.getFaturaDoMes(2L, "ABRIL/2026", "tenantA")).thenReturn(new BigDecimal("700.00"));
        when(lancamentoCartaoRepository.getFaturaDoMesTerceiros(2L, "ABRIL/2026", "tenantA")).thenReturn(new BigDecimal("200.00"));

        FechamentoMensalResponseDTO resumo = fechamentoMensalService.obterResumoMensal(2026, 4);

        assertThat(resumo.getDespesasDoMes()).isEqualByComparingTo("1500.00");
        assertThat(resumo.getTotalFaturasProprias()).isEqualByComparingTo("1500.00");
        assertThat(resumo.getTotalFaturasLancadasComoDespesa()).isEqualByComparingTo("1000.00");
        assertThat(resumo.getTotalFaturasNaoLancadas()).isEqualByComparingTo("500.00");
    }

    @Test
    void fecharMesDevePreencherAuditoriaERegistrarHistorico() {
        TenantContext.setTenantId("tenantA");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario-a", null)
        );

        when(fechamentoMensalRepository.findByTenantIdAndAnoAndMes("tenantA", 2026, 3)).thenReturn(Optional.empty());
        when(fechamentoMensalRepository.findByTenantIdAndAnoAndMes("tenantA", 2026, 2)).thenReturn(Optional.empty());
        when(contaCorrenteService.listarTodos()).thenReturn(List.of());
        when(receitaRepository.findByDataRecebimentoBetweenAndTenantId(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "tenantA")
        ).thenReturn(List.of());
        when(despesaRepository.findByTenantIdAndDataVencimentoBetween(
                "tenantA",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31))
        ).thenReturn(List.of());
        when(lancamentoCartaoRepository.findByMesAnoFaturaAndTenantId("MARCO/2026", "tenantA")).thenReturn(List.of());
        when(cartaoCreditoRepository.findByTenantId("tenantA")).thenReturn(List.of());
        when(fechamentoMensalRepository.save(any(FechamentoMensal.class))).thenAnswer(invocation -> {
            FechamentoMensal fechamento = invocation.getArgument(0);
            fechamento.setId(99L);
            return fechamento;
        });

        FechamentoMensalResponseDTO response = fechamentoMensalService.fecharMes(2026, 3);

        assertThat(response.getFechadoPor()).isEqualTo("usuario-a");
        assertThat(response.getFechadoEm()).isNotNull();
        assertThat(response.getUltimoReprocessamentoPor()).isNull();
        verify(fechamentoMensalHistoricoRepository).save(any(FechamentoMensalHistorico.class));
    }

    @Test
    void reabrirMesDeveRegistrarHistorico() {
        TenantContext.setTenantId("tenantA");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario-a", null)
        );

        FechamentoMensal fechamentoMensal = FechamentoMensal.builder()
                .id(55L)
                .tenantId("tenantA")
                .ano(2026)
                .mes(3)
                .build();
        when(fechamentoMensalRepository.findByTenantIdAndAnoAndMes("tenantA", 2026, 3))
                .thenReturn(Optional.of(fechamentoMensal));

        fechamentoMensalService.reabrirMes(2026, 3);

        verify(fechamentoMensalHistoricoRepository).save(any(FechamentoMensalHistorico.class));
    }
}
