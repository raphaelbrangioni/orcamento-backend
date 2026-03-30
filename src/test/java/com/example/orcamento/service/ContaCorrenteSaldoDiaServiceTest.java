package com.example.orcamento.service;

import com.example.orcamento.dto.FecharSaldoDiaRequestDTO;
import com.example.orcamento.model.ContaCorrente;
import com.example.orcamento.repository.ContaCorrenteSaldoDiaRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaCorrenteSaldoDiaServiceTest {

    @Mock
    private ContaCorrenteService contaCorrenteService;
    @Mock
    private ContaCorrenteSaldoDiaRepository contaCorrenteSaldoDiaRepository;
    @Mock
    private MovimentacaoRepository movimentacaoRepository;

    @InjectMocks
    private ContaCorrenteSaldoDiaService contaCorrenteSaldoDiaService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void fecharDiaDeveBloquearFimDeSemana() {
        TenantContext.setTenantId("tenantA");
        ContaCorrente conta = new ContaCorrente(1L, "0001", "12345", "001", "Banco X", BigDecimal.ZERO, 1L, "tenantA", true);
        when(contaCorrenteService.buscarPorId(1L)).thenReturn(Optional.of(conta));

        FecharSaldoDiaRequestDTO request = new FecharSaldoDiaRequestDTO();
        request.setData(LocalDate.of(2026, 3, 28));

        assertThatThrownBy(() -> contaCorrenteSaldoDiaService.fecharDia(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sabado ou domingo");
    }

    @Test
    void fecharDiaDeveExigirUltimoDiaUtilAnteriorFechado() {
        TenantContext.setTenantId("tenantA");
        ContaCorrente conta = new ContaCorrente(1L, "0001", "12345", "001", "Banco X", BigDecimal.ZERO, 1L, "tenantA", true);
        when(contaCorrenteService.buscarPorId(1L)).thenReturn(Optional.of(conta));
        when(contaCorrenteSaldoDiaRepository.existsByContaCorrenteIdAndTenantIdAndDataLessThan(1L, "tenantA", LocalDate.of(2026, 3, 30)))
                .thenReturn(true);
        when(contaCorrenteSaldoDiaRepository.existsByContaCorrenteIdAndTenantIdAndData(1L, "tenantA", LocalDate.of(2026, 3, 27)))
                .thenReturn(false);

        FecharSaldoDiaRequestDTO request = new FecharSaldoDiaRequestDTO();
        request.setData(LocalDate.of(2026, 3, 30));

        assertThatThrownBy(() -> contaCorrenteSaldoDiaService.fecharDia(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ultimo dia util anterior");
    }
}
