package com.example.orcamento.controller;

import com.example.orcamento.dto.FechamentoMensalHistoricoDTO;
import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.UsuarioRepository;
import com.example.orcamento.security.JwtUtil;
import com.example.orcamento.service.FechamentoMensalService;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FechamentoMensalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private FechamentoMensalService fechamentoMensalService;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void fecharMesRetornaResumo() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        when(fechamentoMensalService.fecharMes(2026, 4)).thenReturn(
                FechamentoMensalResponseDTO.builder()
                        .ano(2026)
                        .mes(4)
                        .fechado(true)
                        .saldoFinal(new BigDecimal("1234.56"))
                        .build()
        );

        mockMvc.perform(post("/api/v1/fechamentos-mensais/2026/4/fechar")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.mes").value(4))
                .andExpect(jsonPath("$.fechado").value(true))
                .andExpect(jsonPath("$.saldoFinal").value(1234.56));
    }

    @Test
    void listarFechamentosRetornaLista() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        when(fechamentoMensalService.listarFechamentos(2026)).thenReturn(List.of(
                FechamentoMensalResponseDTO.builder().ano(2026).mes(4).fechado(true).build(),
                FechamentoMensalResponseDTO.builder().ano(2026).mes(3).fechado(true).build()
        ));

        mockMvc.perform(get("/api/v1/fechamentos-mensais")
                        .param("ano", "2026")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mes").value(4))
                .andExpect(jsonPath("$[1].mes").value(3));
    }

    @Test
    void reabrirFechamentoRetornaNoContent() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        doNothing().when(fechamentoMensalService).reabrirMes(2026, 4);

        mockMvc.perform(delete("/api/v1/fechamentos-mensais/2026/4")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isNoContent());
    }

    @Test
    void listarHistoricoRetornaEventos() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        when(fechamentoMensalService.listarHistorico(2026, 4)).thenReturn(List.of(
                FechamentoMensalHistoricoDTO.builder()
                        .id(1L)
                        .ano(2026)
                        .mes(4)
                        .evento("FECHADO")
                        .username("usuario-a")
                        .build()
        ));

        mockMvc.perform(get("/api/v1/fechamentos-mensais/2026/4/historico")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evento").value("FECHADO"))
                .andExpect(jsonPath("$[0].username").value("usuario-a"));
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
