package com.example.orcamento.service;

import com.example.orcamento.model.Pessoa;
import com.example.orcamento.repository.PessoaRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PessoaServiceMultiTenantTest {

    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private PessoaService pessoaService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findAllDeveConsultarPessoasSomenteDoTenantAtual() {
        TenantContext.setTenantId("tenantA");
        when(pessoaRepository.findByTenantId("tenantA")).thenReturn(List.of(Pessoa.builder().id(1L).nome("Ana").tenantId("tenantA").build()));

        List<Pessoa> resultado = pessoaService.findAll();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTenantId()).isEqualTo("tenantA");
        verify(pessoaRepository).findByTenantId("tenantA");
    }

    @Test
    void saveDeveAtribuirTenantAtualAutomaticamente() {
        TenantContext.setTenantId("tenantA");
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pessoa pessoa = Pessoa.builder().nome("Carlos").build();
        Pessoa salva = pessoaService.save(pessoa);

        assertThat(salva.getTenantId()).isEqualTo("tenantA");
    }
}
