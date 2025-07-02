package com.example.orcamento.service;

import com.example.orcamento.model.Usuario;
import com.example.orcamento.repository.UsuarioRepository;
import com.example.orcamento.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder; // ✅ Agora é reconhecido como um Bean

    public String authenticate(String username, String password) {
        // Buscar usuário no banco
        Usuario user = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Dados Invalidos"));

        // Verifica a senha (agora usando `passwordEncoder.matches()`)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Usuário ou senha inválidos");
        }

        // Bloqueia login se não validou o token do primeiro login
        if (user.isPrimeiroLogin() || user.getDataPrimeiroLogin() == null) {
            throw new IllegalStateException("Primeiro acesso: valide o token enviado antes de acessar o sistema.");
        }

        // Gera o token JWT incluindo o tenantId
        return jwtUtil.generateToken(username, user.getTenantId());
    }

    public Usuario criarUsuario(String username, String senha, String email, String tenantId) {
        // Verifica se o usuário já existe
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(passwordEncoder.encode(senha)); // 🔐 Criptografa a senha
        usuario.setTenantId(tenantId);

        return usuarioRepository.save(usuario);
    }

    public Usuario criarUsuario(String username, String password, String nome, String email, String tenantId, boolean ativo, boolean admin, boolean primeiroLogin, String token) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setTenantId(tenantId);
        usuario.setAtivo(ativo);
        usuario.setAdmin(admin);
        usuario.setPrimeiroLogin(primeiroLogin);
        usuario.setToken(token);
        usuario.setDataCadastro(LocalDateTime.now());
        usuario.setDataPrimeiroLogin(null);
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario obterUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    public Usuario obterUsuarioPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    public void atualizarSenha(String username, String novaSenha) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        usuario.setPassword(passwordEncoder.encode(novaSenha)); // Criptografa a nova senha
        usuarioRepository.save(usuario);
    }

    public Usuario atualizarUsuario(Long id, Usuario usuarioAtualizado) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        usuario.setUsername(usuarioAtualizado.getUsername());
        usuario.setEmail(usuarioAtualizado.getEmail());
        usuario.setTenantId(usuarioAtualizado.getTenantId());
        // Só atualiza a senha se vier preenchida
        if (usuarioAtualizado.getPassword() != null && !usuarioAtualizado.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuarioAtualizado.getPassword()));
        }
        usuario.setAtivo(usuarioAtualizado.isAtivo());
        usuario.setAdmin(usuarioAtualizado.isAdmin());
        return usuarioRepository.save(usuario);
    }

    public boolean validarTokenPrimeiroLogin(String username, String tokenInformado) {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        if (usuario.isPrimeiroLogin() && usuario.getToken() != null && usuario.getToken().equals(tokenInformado)) {
            usuario.setPrimeiroLogin(false);
            usuario.setToken(null); // Remove o token após uso
            usuario.setDataPrimeiroLogin(LocalDateTime.now());
            usuarioRepository.save(usuario);
            return true;
        }
        return false;
    }
}
