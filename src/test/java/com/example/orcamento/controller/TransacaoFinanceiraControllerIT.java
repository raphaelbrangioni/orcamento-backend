package com.example.orcamento.controller;

import com.example.orcamento.OrcamentoApplication;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.StatusCartao;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.MovimentacaoRepository;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = OrcamentoApplication.class)
@AutoConfigureMockMvc
@Transactional
class TransacaoFinanceiraControllerIT {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LancamentoCartaoRepository lancamentoCartaoRepository;
    @Autowired
    private CartaoCreditoRepository cartaoCreditoRepository;
    @Autowired
    private DespesaRepository despesaRepository;
    @Autowired
    private MovimentacaoRepository movimentacaoRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // Limpeza total das tabelas para evitar dados órfãos
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE movimentacoes").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE lancamentos_cartao").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE despesas").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE cartoes_credito").executeUpdate();
        entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @WithMockUser(username = "teste", roles = {"USER"})
    void usuarioDeUmTenantNaoVêLancamentosDeOutroTenant() throws Exception {
        // Cria cartões de crédito para cada tenant
        CartaoCredito cartaoA = new CartaoCredito();
        cartaoA.setNome("Cartão A");
        cartaoA.setLimite(BigDecimal.valueOf(1000));
        cartaoA.setDiaVencimento(5);
        cartaoA.setStatus(StatusCartao.ATIVO);
        cartaoA.setModeloImportacao("generico");
        cartaoA.setTenantId("tenantA");
        cartaoA = cartaoCreditoRepository.save(cartaoA);
        cartaoCreditoRepository.flush();
        System.out.println("CartaoA salvo com ID: " + cartaoA.getId());

        CartaoCredito cartaoB = new CartaoCredito();
        cartaoB.setNome("Cartão B");
        cartaoB.setLimite(BigDecimal.valueOf(2000));
        cartaoB.setDiaVencimento(10);
        cartaoB.setStatus(StatusCartao.ATIVO);
        cartaoB.setModeloImportacao("generico");
        cartaoB.setTenantId("tenantB");
        cartaoB = cartaoCreditoRepository.save(cartaoB);
        cartaoCreditoRepository.flush();
        System.out.println("CartaoB salvo com ID: " + cartaoB.getId());

        // Cria lançamentos para dois tenants diferentes
        LancamentoCartao lancamentoA = new LancamentoCartao();
        lancamentoA.setTenantId("tenantA");
        lancamentoA.setDescricao("Compra A");
        lancamentoA.setCartaoCredito(cartaoA);
        lancamentoA.setValorTotal(BigDecimal.valueOf(100));
        lancamentoA.setParcelaAtual(1);
        lancamentoA.setTotalParcelas(1);
        lancamentoA.setMesAnoFatura("2025-07");
        lancamentoA.setDataCompra(LocalDate.now());
        lancamentoA.setProprietario("Própria");
        lancamentoCartaoRepository.save(lancamentoA);

        LancamentoCartao lancamentoB = new LancamentoCartao();
        lancamentoB.setTenantId("tenantB");
        lancamentoB.setDescricao("Compra B");
        lancamentoB.setCartaoCredito(cartaoB);
        lancamentoB.setValorTotal(BigDecimal.valueOf(200));
        lancamentoB.setParcelaAtual(1);
        lancamentoB.setTotalParcelas(1);
        lancamentoB.setMesAnoFatura("2025-07");
        lancamentoB.setDataCompra(LocalDate.now());
        lancamentoB.setProprietario("Própria");
        lancamentoCartaoRepository.save(lancamentoB);

        // Simula requisição do tenantA
        TenantContext.setTenantId("tenantA");
        MvcResult resultA = mockMvc.perform(get("/api/v1/transacoes/filtrar-dinamico")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andReturn();
        String responseA = resultA.getResponse().getContentAsString();
        assertThat(responseA).contains("Compra A");
        assertThat(responseA).doesNotContain("Compra B");

        // Simula requisição do tenantB
        TenantContext.setTenantId("tenantB");
        MvcResult resultB = mockMvc.perform(get("/api/v1/transacoes/filtrar-dinamico")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andReturn();
        String responseB = resultB.getResponse().getContentAsString();
        assertThat(responseB).contains("Compra B");
        assertThat(responseB).doesNotContain("Compra A");
    }
}
