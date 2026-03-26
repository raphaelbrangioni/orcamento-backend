package com.example.orcamento.service;

import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.UsuarioRepository;
import com.example.orcamento.security.JwtUtil;
import com.example.orcamento.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceMultiTenantTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private UsuarioService usuarioService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void listarUsuariosDeveConsultarSomenteOTenantAtual() {
        TenantContext.setTenantId("tenantA");
        Usuario usuario = usuario("tenantA");
        when(usuarioRepository.findByTenantId("tenantA")).thenReturn(List.of(usuario));

        List<Usuario> resultado = usuarioService.listarUsuarios();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTenantId()).isEqualTo("tenantA");
    }

    @Test
    void obterUsuarioPorIdDeveFalharQuandoUsuarioNaoPertencerAoTenantAtual() {
        TenantContext.setTenantId("tenantA");
        when(usuarioRepository.findByIdAndTenantId(1L, "tenantA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obterUsuarioPorId(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Usuario");
    }

    @Test
    void obterUsuarioPorUsernameDoTenantDeveConsultarComTenantAtual() {
        TenantContext.setTenantId("tenantA");
        Usuario usuario = usuario("tenantA");
        when(usuarioRepository.findByUsernameAndTenantId("ana", "tenantA")).thenReturn(Optional.of(usuario));

        Usuario resultado = usuarioService.obterUsuarioPorUsernameDoTenant("ana");

        assertThat(resultado.getTenantId()).isEqualTo("tenantA");
        verify(usuarioRepository).findByUsernameAndTenantId("ana", "tenantA");
    }

    @Test
    void atualizarUsuarioDeveFalharQuandoUsuarioNaoPertencerAoTenantAtual() {
        TenantContext.setTenantId("tenantA");
        when(usuarioRepository.findByIdAndTenantId(2L, "tenantA")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.atualizarUsuario(2L, new Usuario()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Usuario");
    }

    private Usuario usuario(String tenantId) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("ana");
        usuario.setTenantId(tenantId);
        usuario.setEmail("ana@email.com");
        return usuario;
    }
}
