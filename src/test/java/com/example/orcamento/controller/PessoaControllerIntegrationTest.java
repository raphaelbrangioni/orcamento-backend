package com.example.orcamento.controller;

import com.example.orcamento.dto.PessoaDTO;
import com.example.orcamento.model.Pessoa;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.PessoaRepository;
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
class PessoaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        pessoaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void listarPessoasRetornaApenasRegistrosDoTenantDoToken() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        criarUsuario("usuario-b", "tenantB");
        pessoaRepository.saveAll(List.of(
                pessoa("Pessoa Tenant A", "tenantA"),
                pessoa("Pessoa Tenant B", "tenantB")
        ));

        mockMvc.perform(get("/api/v1/pessoas")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Pessoa Tenant A"));
    }

    @Test
    void buscarPessoaDeOutroTenantRetornaNotFound() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        Pessoa pessoaTenantB = pessoaRepository.save(pessoa("Pessoa Tenant B", "tenantB"));

        mockMvc.perform(get("/api/v1/pessoas/{id}", pessoaTenantB.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pessoa nao encontrada com o id: " + pessoaTenantB.getId()));
    }

    @Test
    void criarPessoaPersisteTenantDoToken() throws Exception {
        criarUsuario("usuario-a", "tenantA");
        PessoaDTO request = new PessoaDTO();
        request.setNome("Nova Pessoa");
        request.setObservacao("Criada via teste");
        request.setAtivo(true);

        mockMvc.perform(post("/api/v1/pessoas")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken("usuario-a", "tenantA"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Nova Pessoa"));

        List<Pessoa> pessoas = pessoaRepository.findByTenantId("tenantA");
        assertThat(pessoas).hasSize(1);
        assertThat(pessoas.get(0).getTenantId()).isEqualTo("tenantA");
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

    private Pessoa pessoa(String nome, String tenantId) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(nome);
        pessoa.setObservacao("teste");
        pessoa.setAtivo(true);
        pessoa.setTenantId(tenantId);
        return pessoa;
    }
}
