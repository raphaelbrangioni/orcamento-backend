package com.example.orcamento.service;

import com.example.orcamento.model.HorasTrabalhadas;
import com.example.orcamento.repository.HorasTrabalhadasRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HorasTrabalhadasServiceMultiTenantTest {

    @Mock
    private HorasTrabalhadasRepository repository;

    @InjectMocks
    private HorasTrabalhadasService horasTrabalhadasService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void registrarDeveAtribuirTenantAtual() {
        TenantContext.setTenantId("tenantA");
        HorasTrabalhadas horas = new HorasTrabalhadas();
        when(repository.save(horas)).thenReturn(horas);

        HorasTrabalhadas resultado = horasTrabalhadasService.registrar(horas);

        assertThat(resultado.getTenantId()).isEqualTo("tenantA");
    }

    @Test
    void atualizarDeveFalharQuandoRegistroForDeOutroTenant() {
        TenantContext.setTenantId("tenantA");
        HorasTrabalhadas existente = new HorasTrabalhadas();
        existente.setId(1L);
        existente.setTenantId("tenantB");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> horasTrabalhadasService.atualizar(1L, new HorasTrabalhadas()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado");
    }

    @Test
    void excluirDeveFalharQuandoRegistroForDeOutroTenant() {
        TenantContext.setTenantId("tenantA");
        HorasTrabalhadas existente = new HorasTrabalhadas();
        existente.setId(2L);
        existente.setTenantId("tenantB");
        when(repository.findById(2L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> horasTrabalhadasService.excluir(2L))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado");

        verify(repository).findById(2L);
    }
}
