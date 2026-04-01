package com.example.orcamento.controller;

import com.example.orcamento.dto.GeracaoFaturaCartaoResponseDTO;
import com.example.orcamento.model.StatusGeracaoFaturaCartao;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.UsuarioRepository;
import com.example.orcamento.security.JwtUtil;
import com.example.orcamento.service.GeracaoFaturaCartaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GeracaoFaturaCartaoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private GeracaoFaturaCartaoService geracaoFaturaCartaoService;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void gerarFaturaRetornaResumo() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        when(geracaoFaturaCartaoService.gerarFatura(1L, 2026, 4))
                .thenReturn(GeracaoFaturaCartaoResponseDTO.builder()
                        .id(10L)
                        .cartaoCreditoId(1L)
                        .ano(2026)
                        .mes(4)
                        .valorProprio(new BigDecimal("1300.00"))
                        .status(StatusGeracaoFaturaCartao.GERADA)
                        .build());

        mockMvc.perform(post("/api/v1/geracoes-fatura/1/2026/4")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.cartaoCreditoId").value(1))
                .andExpect(jsonPath("$.status").value("GERADA"))
                .andExpect(jsonPath("$.valorProprio").value(1300.00));
    }

    @Test
    void buscarGeracaoRetornaRegistro() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        when(geracaoFaturaCartaoService.buscarGeracao(1L, 2026, 4))
                .thenReturn(Optional.of(GeracaoFaturaCartaoResponseDTO.builder()
                        .id(10L)
                        .cartaoCreditoId(1L)
                        .geradoPor("usuario-a")
                        .build()));

        mockMvc.perform(get("/api/v1/geracoes-fatura/1/2026/4")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.geradoPor").value("usuario-a"));
    }

    @Test
    void listarGeracoesDoMesRetornaTodosOsCartoesGerados() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        when(geracaoFaturaCartaoService.listarGeracoes(2026, 4))
                .thenReturn(List.of(
                        GeracaoFaturaCartaoResponseDTO.builder()
                                .id(10L)
                                .cartaoCreditoId(1L)
                                .nomeCartao("Visa")
                                .status(StatusGeracaoFaturaCartao.GERADA)
                                .build(),
                        GeracaoFaturaCartaoResponseDTO.builder()
                                .id(11L)
                                .cartaoCreditoId(2L)
                                .nomeCartao("Master")
                                .status(StatusGeracaoFaturaCartao.REPROCESSADA)
                                .build()
                ));

        mockMvc.perform(get("/api/v1/geracoes-fatura")
                        .param("ano", "2026")
                        .param("mes", "4")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nomeCartao").value("Visa"))
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].status").value("REPROCESSADA"));
    }

    @Test
    void ajustarGeracaoRetornaRegistroAtualizado() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        when(geracaoFaturaCartaoService.ajustarGeracao(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(GeracaoFaturaCartaoResponseDTO.builder()
                        .id(10L)
                        .cartaoCreditoId(1L)
                        .valorTerceiros(new BigDecimal("300.00"))
                        .valorProprio(new BigDecimal("1200.00"))
                        .observacao("Ajuste manual")
                        .ajustadoPor("usuario-a")
                        .build());

        mockMvc.perform(patch("/api/v1/geracoes-fatura/10")
                        .contentType("application/json")
                        .content("""
                                {
                                  "valorTerceiros": 300.00,
                                  "observacao": "Ajuste manual"
                                }
                                """)
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.valorTerceiros").value(300.00))
                .andExpect(jsonPath("$.valorProprio").value(1200.00))
                .andExpect(jsonPath("$.observacao").value("Ajuste manual"))
                .andExpect(jsonPath("$.ajustadoPor").value("usuario-a"));
    }

    private String bearerToken(String username, String tenantId) {
        return "Bearer " + jwtUtil.generateToken(username, tenantId);
    }

    private void criarUsuario(String username, String tenantId) {
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword("senha-teste");
        usuario.setEmail(username + "@teste.local");
        usuario.setNome(username);
        usuario.setTenantId(tenantId);
        usuario.setAtivo(true);
        usuario.setAdmin(false);
        usuario.setPrimeiroLogin(false);
        usuarioRepository.save(usuario);
    }
}
