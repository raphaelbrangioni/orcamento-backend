package com.example.orcamento.service;

import com.example.orcamento.model.Receita;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.repository.ReceitaRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceitaServiceMultiTenantTest {

    @Mock
    private ReceitaRepository receitaRepository;
    @Mock
    private MovimentacaoService movimentacaoService;
    @Mock
    private MovimentacaoRepository movimentacaoRepository;
    @Mock
    private ContaCorrenteService contaCorrenteService;

    @InjectMocks
    private ReceitaService receitaService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void efetivarReceitaDeveFalharQuandoReceitaNaoPertenceAoTenantAtual() {
        TenantContext.setTenantId("tenantA");
        when(receitaRepository.findByIdAndTenantId(7L, "tenantA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> receitaService.efetivarReceita(7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Receita");
    }

    @Test
    void listarReceitasPorMesDeveConsultarSomenteReceitasDoTenantAtual() {
        TenantContext.setTenantId("tenantA");
        Receita receita = Receita.builder()
                .id(1L)
                .tenantId("tenantA")
                .descricao("Salario")
                .dataRecebimento(LocalDate.of(2026, 3, 10))
                .isPrevista(false)
                .valor(java.math.BigDecimal.TEN)
                .build();
        when(receitaRepository.findByTenantId("tenantA")).thenReturn(List.of(receita));

        Map<String, Map<String, java.math.BigDecimal>> resultado = receitaService.listarReceitasPorMes(2026);

        assertThat(resultado).containsKey("MARCH");
        verify(receitaRepository).findByTenantId("tenantA");
    }
}
