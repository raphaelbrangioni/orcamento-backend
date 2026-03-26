package com.example.orcamento.controller;

import com.example.orcamento.dto.CompraDTO;
import com.example.orcamento.dto.CompraTerceiroDTO;
import com.example.orcamento.model.CartaoCredito;
import com.example.orcamento.model.CategoriaDespesa;
import com.example.orcamento.model.Compra;
import com.example.orcamento.model.Pessoa;
import com.example.orcamento.model.StatusCartao;
import com.example.orcamento.model.SubcategoriaDespesa;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.CartaoCreditoRepository;
import com.example.orcamento.repository.CategoriaDespesaRepository;
import com.example.orcamento.repository.CompraRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import com.example.orcamento.repository.PessoaRepository;
import com.example.orcamento.repository.SubcategoriaDespesaRepository;
import com.example.orcamento.repository.UsuarioRepository;
import com.example.orcamento.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompraControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private LancamentoCartaoRepository lancamentoCartaoRepository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private SubcategoriaDespesaRepository subcategoriaDespesaRepository;

    @Autowired
    private CategoriaDespesaRepository categoriaDespesaRepository;

    @Autowired
    private CartaoCreditoRepository cartaoCreditoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        lancamentoCartaoRepository.deleteAll();
        compraRepository.deleteAll();
        pessoaRepository.deleteAll();
        subcategoriaDespesaRepository.deleteAll();
        categoriaDespesaRepository.deleteAll();
        cartaoCreditoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void listarComprasRetornaApenasTenantDoToken() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        criarUsuario("usuario-b", "tenantB");

        CartaoCredito cartaoA = salvarCartao("Cartao A", "tenantA");
        CartaoCredito cartaoB = salvarCartao("Cartao B", "tenantB");
        SubcategoriaDespesa subcategoriaA = salvarSubcategoria(101L, 1L, "Moradia", "tenantA");
        SubcategoriaDespesa subcategoriaB = salvarSubcategoria(201L, 2L, "Lazer", "tenantB");

        compraRepository.save(compra("Compra Tenant A", cartaoA, subcategoriaA, "tenantA"));
        compraRepository.save(compra("Compra Tenant B", cartaoB, subcategoriaB, "tenantB"));

        mockMvc.perform(get("/api/v1/compras")
                        .param("page", "0")
                        .param("size", "10")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].descricao").value("Compra Tenant A"));
    }

    @Test
    void cadastrarCompraComPessoaDeOutroTenantRetornaNotFound() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        CartaoCredito cartaoA = salvarCartao("Cartao A", "tenantA");
        SubcategoriaDespesa subcategoriaA = salvarSubcategoria(101L, 1L, "Moradia", "tenantA");
        Pessoa pessoaTenantB = pessoaRepository.save(pessoa("Terceiro B", "tenantB"));

        CompraDTO request = novaCompraRequest(cartaoA.getId(), subcategoriaA.getId(), pessoaTenantB.getId());

        mockMvc.perform(post("/api/v1/compras/parceladas")
                        .param("mesPrimeiraParcela", "ABRIL")
                        .param("numeroParcelas", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pessoa nao encontrada: " + pessoaTenantB.getId()));

        assertThat(compraRepository.findAll()).isEmpty();
        assertThat(lancamentoCartaoRepository.findAll()).isEmpty();
    }

    @Test
    void cadastrarCompraComReferenciasDoMesmoTenantCriaCompraEParcelas() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        CartaoCredito cartaoA = salvarCartao("Cartao A", "tenantA");
        SubcategoriaDespesa subcategoriaA = salvarSubcategoria(101L, 1L, "Moradia", "tenantA");
        Pessoa pessoaTenantA = pessoaRepository.save(pessoa("Terceiro A", "tenantA"));

        CompraDTO request = novaCompraRequest(cartaoA.getId(), subcategoriaA.getId(), pessoaTenantA.getId());

        mockMvc.perform(post("/api/v1/compras/parceladas")
                        .param("mesPrimeiraParcela", "ABRIL")
                        .param("numeroParcelas", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Notebook"))
                .andExpect(jsonPath("$.cartaoCreditoId").value(cartaoA.getId()))
                .andExpect(jsonPath("$.subcategoriaId").value(subcategoriaA.getId()));

        assertThat(compraRepository.findAll()).hasSize(1);
        assertThat(compraRepository.findAll().get(0).getTenantId()).isEqualTo("tenantA");
        assertThat(lancamentoCartaoRepository.findAll()).hasSize(2);
        assertThat(lancamentoCartaoRepository.findAll())
                .allMatch(lancamento -> "tenantA".equals(lancamento.getTenantId()));
    }

    private String bearerToken(String username, String tenantId) {
        return "Bearer " + jwtUtil.generateToken(username, tenantId);
    }

    private Usuario criarUsuario(String username, String tenantId) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword("senha-teste");
        usuario.setEmail(username + "@teste.local");
        usuario.setNome(username);
        usuario.setTenantId(tenantId);
        usuario.setAtivo(true);
        usuario.setAdmin(false);
        usuario.setPrimeiroLogin(false);
        return usuarioRepository.save(usuario);
    }

    private CartaoCredito salvarCartao(String nome, String tenantId) {
        CartaoCredito cartao = new CartaoCredito();
        cartao.setNome(nome);
        cartao.setLimite(BigDecimal.valueOf(5000));
        cartao.setDiaVencimento(10);
        cartao.setStatus(StatusCartao.ATIVO);
        cartao.setModeloImportacao("generico");
        cartao.setTenantId(tenantId);
        return cartaoCreditoRepository.save(cartao);
    }

    private SubcategoriaDespesa salvarSubcategoria(Long subcategoriaId, Long categoriaId, String nome, String tenantId) {
        CategoriaDespesa categoria = new CategoriaDespesa();
        categoria.setId(categoriaId);
        categoria.setNome("Categoria " + categoriaId);
        categoria.setTenantId(tenantId);
        categoriaDespesaRepository.save(categoria);

        SubcategoriaDespesa subcategoria = new SubcategoriaDespesa();
        subcategoria.setId(subcategoriaId);
        subcategoria.setNome(nome);
        subcategoria.setTenantId(tenantId);
        subcategoria.setCategoria(categoria);
        return subcategoriaDespesaRepository.save(subcategoria);
    }

    private Pessoa pessoa(String nome, String tenantId) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(nome);
        pessoa.setObservacao("teste");
        pessoa.setAtivo(true);
        pessoa.setTenantId(tenantId);
        return pessoa;
    }

    private Compra compra(String descricao, CartaoCredito cartao, SubcategoriaDespesa subcategoria, String tenantId) {
        Compra compra = new Compra();
        compra.setDescricao(descricao);
        compra.setValorTotal(BigDecimal.valueOf(300));
        compra.setNumeroParcelas(1);
        compra.setDataCompra(LocalDate.of(2026, 3, 10));
        compra.setCartaoCredito(cartao);
        compra.setSubcategoria(subcategoria);
        compra.setProprietario("PROPRIA");
        compra.setDetalhes("teste");
        compra.setTenantId(tenantId);
        return compra;
    }

    private CompraDTO novaCompraRequest(Long cartaoId, Long subcategoriaId, Long pessoaId) {
        CompraTerceiroDTO terceiro = new CompraTerceiroDTO();
        terceiro.setPessoaId(pessoaId);
        terceiro.setPercentual(BigDecimal.valueOf(50));

        CompraDTO request = new CompraDTO();
        request.setDescricao("Notebook");
        request.setValorTotal(BigDecimal.valueOf(2400));
        request.setNumeroParcelas(2);
        request.setDataCompra(LocalDate.of(2026, 3, 20));
        request.setCartaoCreditoId(cartaoId);
        request.setSubcategoriaId(subcategoriaId);
        request.setProprietario("PROPRIA");
        request.setDetalhes("Compra de teste");
        request.setTerceiros(List.of(terceiro));
        return request;
    }
}
