package com.example.orcamento.controller;

import com.example.orcamento.dto.conciliacao.ConciliacaoOfxProcessamentoDTO;
import com.example.orcamento.dto.conciliacao.ConciliacaoOfxRelatorioDTO;
import com.example.orcamento.dto.conciliacao.MovimentoOfxDTO;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.UsuarioRepository;
import com.example.orcamento.security.JwtUtil;
import com.example.orcamento.service.ConciliacaoOfxProcessamentoService;
import com.example.orcamento.service.ConciliacaoOfxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConciliacaoOfxControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockBean
    private ConciliacaoOfxService conciliacaoOfxService;
    @MockBean
    private ConciliacaoOfxProcessamentoService conciliacaoOfxProcessamentoService;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
    }

    @Test
    void conciliarDespesasPagasRetornaRelatorioEsperado() throws Exception {
        criarUsuario("usuario-a", "tenantA");

        ConciliacaoOfxRelatorioDTO response = ConciliacaoOfxRelatorioDTO.builder()
                .contaCorrenteId(1L)
                .periodoInicio(LocalDate.of(2026, 3, 1))
                .periodoFim(LocalDate.of(2026, 3, 31))
                .toleranciaDias(2)
                .bancoIdOfx("077")
                .contaIdOfx("10962115")
                .bancoSemPagamento(List.of(
                        MovimentoOfxDTO.builder()
                                .data(LocalDate.of(2026, 3, 10))
                                .valor(new BigDecimal("-150.75"))
                                .memo("PAGAMENTO XYZ")
                                .fitId("ABC123")
                                .build()
                ))
                .conciliados(List.of())
                .pagamentoSemBanco(List.of())
                .ambiguos(List.of())
                .receitasConciliadas(List.of())
                .transferenciasConciliadas(List.of())
                .creditosOrigemCartao(List.of())
                .bancoSemReceita(List.of())
                .receitaSemBanco(List.of())
                .receitasAmbiguas(List.of())
                .build();

        when(conciliacaoOfxService.conciliar(eq(1L), eq(2), eq(new BigDecimal("0.00")), eq(new BigDecimal("1000.00")), any()))
                .thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extrato.ofx",
                "application/x-ofx",
                "<OFX></OFX>".getBytes(StandardCharsets.ISO_8859_1)
        );

        mockMvc.perform(multipart("/api/v1/conciliacao/ofx/despesas-pagas")
                        .file(file)
                        .param("contaCorrenteId", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contaCorrenteId").value(1))
                .andExpect(jsonPath("$.bancoIdOfx").value("077"))
                .andExpect(jsonPath("$.bancoSemPagamento[0].fitId").value("ABC123"));
    }

    @Test
    void conciliarExtratoUsaMesmoContrato() throws Exception {
        criarUsuario("usuario-a", "tenantA");

        ConciliacaoOfxRelatorioDTO response = ConciliacaoOfxRelatorioDTO.builder()
                .contaCorrenteId(2L)
                .periodoInicio(LocalDate.of(2026, 3, 1))
                .periodoFim(LocalDate.of(2026, 3, 30))
                .toleranciaDias(3)
                .toleranciaValor(new BigDecimal("5.00"))
                .toleranciaValorMinimo(new BigDecimal("1500.00"))
                .bancoIdOfx("341")
                .contaIdOfx("7028056203")
                .conciliados(List.of())
                .bancoSemPagamento(List.of())
                .pagamentoSemBanco(List.of())
                .ambiguos(List.of())
                .receitasConciliadas(List.of())
                .transferenciasConciliadas(List.of())
                .creditosOrigemCartao(List.of())
                .bancoSemReceita(List.of())
                .receitaSemBanco(List.of())
                .receitasAmbiguas(List.of())
                .build();

        when(conciliacaoOfxService.conciliar(eq(2L), eq(3), eq(new BigDecimal("5.00")), eq(new BigDecimal("1500.00")), any()))
                .thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extrato.ofx",
                "application/x-ofx",
                "<OFX></OFX>".getBytes(StandardCharsets.ISO_8859_1)
        );

        mockMvc.perform(multipart("/api/v1/conciliacao/ofx/extrato")
                        .file(file)
                        .param("contaCorrenteId", "2")
                        .param("toleranciaDias", "3")
                        .param("toleranciaValor", "5.00")
                        .param("toleranciaValorMinimo", "1500.00")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contaCorrenteId").value(2))
                .andExpect(jsonPath("$.toleranciaDias").value(3))
                .andExpect(jsonPath("$.bancoIdOfx").value("341"));
    }

    @Test
    void listarProcessamentosRetornaHistoricoFiltrado() throws Exception {
        criarUsuario("usuario-a", "tenantA");

        when(conciliacaoOfxProcessamentoService.inicioDoDia(LocalDate.of(2026, 3, 1)))
                .thenReturn(LocalDateTime.of(2026, 3, 1, 0, 0));
        when(conciliacaoOfxProcessamentoService.fimDoDia(LocalDate.of(2026, 3, 31)))
                .thenReturn(LocalDateTime.of(2026, 3, 31, 23, 59, 59));
        when(conciliacaoOfxProcessamentoService.listarProcessamentos(
                1L,
                "PROCESSADO",
                LocalDateTime.of(2026, 3, 1, 0, 0),
                LocalDateTime.of(2026, 3, 31, 23, 59, 59)
        )).thenReturn(List.of(
                ConciliacaoOfxProcessamentoDTO.builder()
                        .id(10L)
                        .username("usuario-a")
                        .contaCorrenteId(1L)
                        .nomeArquivo("extrato-marco.ofx")
                        .status("PROCESSADO")
                        .conciliadosQuantidade(5)
                        .processadoEm(LocalDateTime.of(2026, 3, 31, 12, 0))
                        .build()
        ));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/conciliacao/ofx/processamentos")
                        .param("contaCorrenteId", "1")
                        .param("status", "PROCESSADO")
                        .param("processadoDe", "2026-03-01")
                        .param("processadoAte", "2026-03-31")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nomeArquivo").value("extrato-marco.ofx"))
                .andExpect(jsonPath("$[0].status").value("PROCESSADO"))
                .andExpect(jsonPath("$[0].conciliadosQuantidade").value(5));
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
