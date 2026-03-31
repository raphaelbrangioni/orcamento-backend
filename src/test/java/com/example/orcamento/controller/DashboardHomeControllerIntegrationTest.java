package com.example.orcamento.controller;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeAlertaDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeContasDTO;
import com.example.orcamento.dto.dashboard.DashboardHomeDTO;
import com.example.orcamento.repository.UsuarioRepository;
import com.example.orcamento.security.JwtUtil;
import com.example.orcamento.service.DashboardHomeService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardHomeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private DashboardHomeService dashboardHomeService;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void obterDashboardRetornaPayloadEsperado() throws Exception {
        criarUsuario("usuario-a", "tenantA");

        DashboardHomeDTO response = DashboardHomeDTO.builder()
                .ano(2026)
                .mes(4)
                .fechamentoMensal(FechamentoMensalResponseDTO.builder()
                        .ano(2026)
                        .mes(4)
                        .fechado(false)
                        .despesasDoMes(new BigDecimal("35063.38"))
                        .totalFaturasNaoLancadas(new BigDecimal("1611.63"))
                        .build())
                .contas(DashboardHomeContasDTO.builder()
                        .quantidadeContasAtivas(8)
                        .saldoTotal(new BigDecimal("7427.75"))
                        .build())
                .totalFaturasCartoes(new BigDecimal("32258.15"))
                .alertas(List.of(
                        DashboardHomeAlertaDTO.builder()
                                .tipo("FATURAS_NAO_LANCADAS")
                                .nivel("warning")
                                .titulo("Existem faturas proprias nao lancadas")
                                .mensagem("Parte das faturas do mes ainda nao foi lancada como despesa.")
                                .quantidade(3)
                                .valor(new BigDecimal("1611.63"))
                                .build()
                ))
                .build();

        when(dashboardHomeService.obterDashboard(2026, 4)).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/home")
                        .param("ano", "2026")
                        .param("mes", "4")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.mes").value(4))
                .andExpect(jsonPath("$.fechamentoMensal.fechado").value(false))
                .andExpect(jsonPath("$.contas.quantidadeContasAtivas").value(8))
                .andExpect(jsonPath("$.alertas[0].tipo").value("FATURAS_NAO_LANCADAS"))
                .andExpect(jsonPath("$.alertas[0].quantidade").value(3));
    }

    private String bearerToken(String username, String tenantId) {
        return "Bearer " + jwtUtil.generateToken(username, tenantId);
    }

    private void criarUsuario(String username, String tenantId) {
        com.example.orcamento.model.Usuario usuario = new com.example.orcamento.model.Usuario();
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
