package com.example.orcamento.service;

import com.example.orcamento.dto.GeracaoFaturaCartaoPatchRequestDTO;
import com.example.orcamento.dto.GeracaoFaturaCartaoResponseDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.GeracaoFaturaCartao;
import com.example.orcamento.model.StatusCartao;
import com.example.orcamento.model.StatusGeracaoFaturaCartao;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.GeracaoFaturaCartaoRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeracaoFaturaCartaoServiceTest {

    @Mock
    private GeracaoFaturaCartaoRepository geracaoFaturaCartaoRepository;
    @Mock
    private CartaoCreditoRepository cartaoCreditoRepository;
    @Mock
    private LancamentoCartaoRepository lancamentoCartaoRepository;
    @Mock
    private DespesaRepository despesaRepository;
    @Mock
    private DespesaService despesaService;
    @Mock
    private SubcategoriaDespesaRepository subcategoriaDespesaRepository;

    @InjectMocks
    private GeracaoFaturaCartaoService geracaoFaturaCartaoService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void gerarFaturaDeveCriarDespesaERegistrarGeracao() {
        TenantContext.setTenantId("tenantA");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario-a", null)
        );

        CartaoCredito cartao = new CartaoCredito();
        cartao.setId(1L);
        cartao.setNome("Visa Infinite");
        cartao.setDiaVencimento(10);
        cartao.setLimite(new BigDecimal("10000"));
        cartao.setStatus(StatusCartao.ATIVO);
        cartao.setTenantId("tenantA");

        CategoriaDespesa categoria = new CategoriaDespesa();
        categoria.setId(18L);
        categoria.setNome("Pagamento de fatura");
        categoria.setTenantId("tenantA");

        SubcategoriaDespesa subcategoria = new SubcategoriaDespesa();
        subcategoria.setId(1801L);
        subcategoria.setNome("Pagamento de fatura");
        subcategoria.setTenantId("tenantA");
        subcategoria.setCategoria(categoria);

        Despesa despesaGerada = Despesa.builder()
                .id(99L)
                .nome("Fatura Cartao Visa Infinite")
                .dataVencimento(LocalDate.of(2026, 4, 10))
                .build();

        when(cartaoCreditoRepository.findByIdAndTenantId(1L, "tenantA")).thenReturn(Optional.of(cartao));
        when(lancamentoCartaoRepository.getFaturaDoMes(1L, "ABRIL/2026", "tenantA")).thenReturn(new BigDecimal("1500.00"));
        when(lancamentoCartaoRepository.getFaturaDoMesTerceiros(1L, "ABRIL/2026", "tenantA")).thenReturn(new BigDecimal("200.00"));
        when(geracaoFaturaCartaoRepository.findByTenantIdAndCartaoCreditoIdAndAnoAndMes("tenantA", 1L, 2026, 4))
                .thenReturn(Optional.empty());
        when(subcategoriaDespesaRepository.findByNomeAndCategoriaNomeAndTenantId(
                "Pagamento de fatura",
                "Pagamento de fatura",
                "tenantA"
        )).thenReturn(Optional.of(subcategoria));
        when(despesaService.salvarDespesa(any(Despesa.class))).thenReturn(despesaGerada);
        when(geracaoFaturaCartaoRepository.save(any(GeracaoFaturaCartao.class))).thenAnswer(invocation -> {
            GeracaoFaturaCartao geracao = invocation.getArgument(0);
            geracao.setId(10L);
            return geracao;
        });

        GeracaoFaturaCartaoResponseDTO response = geracaoFaturaCartaoService.gerarFatura(1L, 2026, 4);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCartaoCreditoId()).isEqualTo(1L);
        assertThat(response.getValorFatura()).isEqualByComparingTo("1500.00");
        assertThat(response.getValorTerceiros()).isEqualByComparingTo("200.00");
        assertThat(response.getValorProprio()).isEqualByComparingTo("1300.00");
        assertThat(response.getDespesaId()).isEqualTo(99L);
        assertThat(response.getStatus()).isEqualTo(StatusGeracaoFaturaCartao.GERADA);
        assertThat(response.getGeradoPor()).isEqualTo("usuario-a");
    }

    @Test
    void ajustarGeracaoDeveAtualizarValorTerceirosObservacaoEDespesa() {
        TenantContext.setTenantId("tenantA");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("usuario-b", null)
        );

        CartaoCredito cartao = new CartaoCredito();
        cartao.setId(1L);
        cartao.setNome("Visa Infinite");
        cartao.setTenantId("tenantA");

        GeracaoFaturaCartao geracao = GeracaoFaturaCartao.builder()
                .id(10L)
                .tenantId("tenantA")
                .cartaoCredito(cartao)
                .ano(2026)
                .mes(4)
                .valorFatura(new BigDecimal("1500.00"))
                .valorTerceiros(new BigDecimal("200.00"))
                .valorProprio(new BigDecimal("1300.00"))
                .despesaId(99L)
                .status(StatusGeracaoFaturaCartao.GERADA)
                .geradoPor("usuario-a")
                .geradoEm(LocalDateTime.of(2026, 3, 31, 20, 0))
                .build();

        Despesa despesa = Despesa.builder()
                .id(99L)
                .nome("Fatura Cartao Visa Infinite")
                .valorPrevisto(new BigDecimal("1300.00"))
                .dataVencimento(LocalDate.of(2026, 4, 10))
                .build();

        when(geracaoFaturaCartaoRepository.findByIdAndTenantId(10L, "tenantA")).thenReturn(Optional.of(geracao));
        when(despesaRepository.findByIdAndTenantId(99L, "tenantA")).thenReturn(Optional.of(despesa));
        when(despesaRepository.save(any(Despesa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(geracaoFaturaCartaoRepository.save(any(GeracaoFaturaCartao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GeracaoFaturaCartaoResponseDTO response = geracaoFaturaCartaoService.ajustarGeracao(
                10L,
                GeracaoFaturaCartaoPatchRequestDTO.builder()
                        .valorTerceiros(new BigDecimal("300.00"))
                        .observacao("Ajuste manual")
                        .build()
        );

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getValorTerceiros()).isEqualByComparingTo("300.00");
        assertThat(response.getValorProprio()).isEqualByComparingTo("1200.00");
        assertThat(response.getObservacao()).isEqualTo("Ajuste manual");
        assertThat(response.getAjustadoPor()).isEqualTo("usuario-b");
        assertThat(response.getAjustadoEm()).isNotNull();
        assertThat(despesa.getValorPrevisto()).isEqualByComparingTo("1200.00");
    }
}
