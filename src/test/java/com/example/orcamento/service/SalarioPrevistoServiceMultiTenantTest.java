package com.example.orcamento.service;

import com.example.orcamento.model.SalarioPrevisto;
import com.example.orcamento.repository.SalarioPrevistoRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalarioPrevistoServiceMultiTenantTest {

    @Mock
    private SalarioPrevistoRepository repository;

    @InjectMocks
    private SalarioPrevistoService salarioPrevistoService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listarSalariosPrevistosDeveConsultarSomenteOTenantAtual() {
        TenantContext.setTenantId("tenantA");
        SalarioPrevisto salario = new SalarioPrevisto(1L, 2026, "MARCO", 1000.0, "tenantA");
        when(repository.findByTenantId("tenantA")).thenReturn(List.of(salario));

        List<SalarioPrevisto> resultado = salarioPrevistoService.listarSalariosPrevistos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTenantId()).isEqualTo("tenantA");
    }

    @Test
    void getSalariosPrevistosPorAnoDeveConsultarSomenteOTenantAtual() {
        TenantContext.setTenantId("tenantA");
        SalarioPrevisto salario = new SalarioPrevisto(1L, 2026, "MARCO", 1000.0, "tenantA");
        when(repository.findByAnoAndTenantId(2026, "tenantA")).thenReturn(List.of(salario));

        Map<String, Double> resultado = salarioPrevistoService.getSalariosPrevistosPorAno(2026);

        assertThat(resultado).containsEntry("MARCO", 1000.0);
    }

    @Test
    void atualizarSalarioDeveFalharQuandoRegistroForDeOutroTenant() {
        TenantContext.setTenantId("tenantA");
        SalarioPrevisto existente = new SalarioPrevisto(5L, 2026, "MARCO", 1000.0, "tenantB");
        when(repository.findById(5L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> salarioPrevistoService.atualizarSalario(5L, new SalarioPrevisto()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado");
    }

    @Test
    void deletarSalarioDeveFalharQuandoRegistroForDeOutroTenant() {
        TenantContext.setTenantId("tenantA");
        SalarioPrevisto existente = new SalarioPrevisto(6L, 2026, "MARCO", 1000.0, "tenantB");
        when(repository.findById(6L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> salarioPrevistoService.deletarSalario(6L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado");

        verify(repository).findById(6L);
    }
}
