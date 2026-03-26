package com.example.orcamento.service;

import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.Compra;
import com.example.orcamento.model.CompraTerceiro;
import com.example.orcamento.model.CompraTerceiroId;
import com.example.orcamento.model.Pessoa;
import com.example.orcamento.model.StatusCartao;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.CompraRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.PessoaRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraServiceMultiTenantTest {

    @Mock
    private CompraRepository compraRepository;
    @Mock
    private LancamentoCartaoRepository lancamentoCartaoRepository;
    @Mock
    private SubcategoriaDespesaRepository subcategoriaDespesaRepository;
    @Mock
    private CartaoCreditoRepository cartaoCreditoRepository;
    @Mock
    private PessoaRepository pessoaRepository;

    @InjectMocks
    private CompraService compraService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void cadastrarCompraParceladaDeveFalharAoVincularPessoaDeOutroTenant() {
        TenantContext.setTenantId("tenantA");

        Compra compra = compraBase();
        CompraTerceiro terceiro = new CompraTerceiro();
        terceiro.setId(new CompraTerceiroId(null, 99L));
        terceiro.setPessoa(Pessoa.builder().id(99L).nome("Terceiro").tenantId("tenantB").build());
        terceiro.setPercentual(BigDecimal.valueOf(50));
        compra.setTerceiros(Set.of(terceiro));

        SubcategoriaDespesa subcategoria = new SubcategoriaDespesa();
        subcategoria.setId(2L);
        subcategoria.setNome("Mercado");
        subcategoria.setTenantId("tenantA");
        when(subcategoriaDespesaRepository.findByIdAndTenantId(2L, "tenantA"))
                .thenReturn(Optional.of(subcategoria));
        when(cartaoCreditoRepository.findByIdAndTenantId(3L, "tenantA"))
                .thenReturn(Optional.of(cartao()));
        when(pessoaRepository.findByIdAndTenantId(99L, "tenantA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.cadastrarCompraParcelada(compra, "MARCO", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pessoa");
    }

    @Test
    void cadastrarCompraParceladaDeveFalharAoVincularSubcategoriaDeOutroTenant() {
        TenantContext.setTenantId("tenantA");

        Compra compra = compraBase();
        when(subcategoriaDespesaRepository.findByIdAndTenantId(2L, "tenantA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.cadastrarCompraParcelada(compra, "MARCO", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Subcategoria");
    }

    private Compra compraBase() {
        Compra compra = new Compra();
        compra.setDescricao("Notebook");
        compra.setValorTotal(BigDecimal.valueOf(1000));
        compra.setNumeroParcelas(2);
        compra.setDataCompra(LocalDate.of(2026, 3, 10));
        compra.setProprietario("Proprio");
        SubcategoriaDespesa subcategoria = new SubcategoriaDespesa();
        subcategoria.setId(2L);
        compra.setSubcategoria(subcategoria);
        compra.setCartaoCredito(cartao());
        return compra;
    }

    private CartaoCredito cartao() {
        CartaoCredito cartao = new CartaoCredito();
        cartao.setId(3L);
        cartao.setNome("Visa");
        cartao.setDiaVencimento(10);
        cartao.setLimite(BigDecimal.valueOf(5000));
        cartao.setStatus(StatusCartao.ATIVO);
        cartao.setTenantId("tenantA");
        return cartao;
    }
}
