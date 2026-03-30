package com.example.orcamento.service;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.Receita;
import com.example.orcamento.model.enums.FormaDePagamento;
import com.example.orcamento.repository.ContaCorrenteSaldoDiaRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.FechamentoMensalRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.ReceitaRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @InjectMocks
    private FechamentoMensalService fechamentoMensalService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
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

        FechamentoMensalResponseDTO resumo = fechamentoMensalService.obterResumoMensal(2026, 3);

        assertThat(resumo.getDespesasPagas()).isEqualByComparingTo("800.00");
        assertThat(resumo.getDespesasPagasNoCaixa()).isEqualByComparingTo("500.00");
        assertThat(resumo.getDespesasPagasCartao()).isEqualByComparingTo("300.00");
        assertThat(resumo.getDespesasPagasNoCaixa().add(resumo.getDespesasPagasCartao()))
                .isEqualByComparingTo(resumo.getDespesasPagas());
        assertThat(resumo.getSaldoFinal()).isEqualByComparingTo("500.00");
    }
}
