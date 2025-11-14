package com.example.orcamento.controller;

import com.example.orcamento.dto.LoginRequest;
import com.example.orcamento.dto.LoginResponse;
import com.example.orcamento.dto.TrocaSenhaRequest;
import com.example.orcamento.dto.UsuarioDTO;
import com.example.orcamento.dto.UsuarioAcessosDTO;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.model.RefreshToken;
import com.example.orcamento.security.JwtUtil;
import com.example.orcamento.service.AcessoUsuarioService;
import com.example.orcamento.service.RefreshTokenService;
import com.example.orcamento.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor  // Lombok injeta automaticamente o service
//@CrossOrigin(origins = "http://localhost:8080")
@CrossOrigin(origins = "http://localhost")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final AcessoUsuarioService acessoUsuarioService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    // 🔹 Login de usuário
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String token = usuarioService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        Usuario usuario = usuarioService.obterUsuarioPorUsername(loginRequest.getUsername());
        // Captura o IP real do usuário
        String ipOrigem = request.getHeader("X-Forwarded-For");
        if (ipOrigem == null || ipOrigem.isBlank()) {
            ipOrigem = loginRequest.getIpAddress(); // pega do frontend se enviado
        }
        if (ipOrigem == null || ipOrigem.isBlank()) {
            ipOrigem = request.getRemoteAddr(); // fallback
        } else {
            ipOrigem = ipOrigem.split(",")[0].trim(); // se vier em cadeia, pega o primeiro
        }
        String tenantId = usuario.getTenantId();
        var acesso = acessoUsuarioService.registrarLogin(usuario, ipOrigem, tenantId);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(usuario);
        return ResponseEntity.ok(Map.of(
            "token", token,
            "acessoId", acesso.getId(),
            "refreshToken", refreshToken.getToken()
        ));
    }

    // ✅ Novo endpoint para registrar usuários
    @PostMapping("/register")
    public ResponseEntity<UsuarioDTO> register(@RequestBody UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioService.criarUsuario(
            usuarioDTO.getUsername(),
            usuarioDTO.getPassword(),
            usuarioDTO.getNome(),
            usuarioDTO.getEmail(),
            usuarioDTO.getTenantId(),
            usuarioDTO.isAtivo(),
            usuarioDTO.isAdmin(),
            usuarioDTO.isPrimeiroLogin(),
            usuarioDTO.getToken()
        );
        return ResponseEntity.ok(new UsuarioDTO(
            usuario.getId(),
            usuario.getUsername(),
            usuario.getNome(),
            null,
            usuario.getEmail(),
            usuario.getTenantId(),
            usuario.isAtivo(),
            usuario.isAdmin(),
            usuario.getToken(),
            usuario.isPrimeiroLogin(),
            usuario.getDataCadastro(),
            usuario.getDataPrimeiroLogin()
        ));
    }

    // Novo registro de usuário com token para primeiro login
    @PostMapping("/register-with-token")
    public ResponseEntity<UsuarioDTO> registerComToken(@RequestBody UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioService.criarUsuario(
            usuarioDTO.getUsername(),
            usuarioDTO.getPassword(),
            usuarioDTO.getNome(),
            usuarioDTO.getEmail(),
            usuarioDTO.getTenantId(),
            usuarioDTO.isAtivo(),
            usuarioDTO.isAdmin(),
            usuarioDTO.isPrimeiroLogin(),
            usuarioDTO.getToken()
        );
        return ResponseEntity.ok(new UsuarioDTO(
            usuario.getId(), usuario.getUsername(), usuario.getNome(), null, usuario.getEmail(), usuario.getTenantId(),
            usuario.isAtivo(), usuario.isAdmin(), usuario.getToken(), usuario.isPrimeiroLogin(), usuario.getDataCadastro(), usuario.getDataPrimeiroLogin()
        ));
    }

    // Método para listar todos os usuários
    @GetMapping("/users")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        List<UsuarioDTO> usuariosDTO = usuarios.stream().map(usuario -> new UsuarioDTO(
            usuario.getId(),
            usuario.getUsername(),
            null, // Nunca retorne a senha!
            usuario.getEmail(),
            usuario.getTenantId(),
            usuario.isAtivo(),
            usuario.isAdmin(),
            usuario.getToken(),
            usuario.isPrimeiroLogin(),
            usuario.getDataCadastro(),
            usuario.getDataPrimeiroLogin()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(usuariosDTO);
    }

    // Método para obter um usuário por ID
    @GetMapping("/users/{id}")
    public ResponseEntity<UsuarioDTO> obterUsuarioPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.obterUsuarioPorId(id);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new UsuarioDTO(usuario.getId(), usuario.getUsername(), usuario.getEmail()));
    }

    // Método para obter um usuário por username
    @GetMapping("/users/username/{username}")
    public ResponseEntity<UsuarioDTO> obterUsuarioPorUsername(@PathVariable String username) {
        Usuario usuario = usuarioService.obterUsuarioPorUsername(username);

        //log.info("Usuário encontrado: {}", usuario);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        UsuarioDTO dto = new UsuarioDTO(
            usuario.getId(),
            usuario.getUsername(),
            usuario.getNome(),
            null, // nunca retorne senha
            usuario.getEmail(),
            usuario.getTenantId(),
            usuario.isAtivo(),
            usuario.isAdmin(),
            usuario.getToken(),
            usuario.isPrimeiroLogin(),
            usuario.getDataCadastro(),
            usuario.getDataPrimeiroLogin()
        );
        return ResponseEntity.ok(dto);
    }

    // Método para atualizar a senha do usuário
    @PutMapping("/users/username/{username}/password")
    public ResponseEntity<String> atualizarSenha(@PathVariable String username, @RequestBody String novaSenha) {
        usuarioService.atualizarSenha(username, novaSenha);
        return ResponseEntity.ok("Senha atualizada com sucesso!");
    }

    // Endpoint seguro para troca de senha
    @PostMapping("/trocar-senha")
    public ResponseEntity<?> trocarSenha(@RequestBody TrocaSenhaRequest request) {
        try {
            // Obtém o usuário autenticado do contexto de segurança
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            log.info("Solicitação de troca de senha para usuário: {}", username);
            // Autentica a senha atual
            usuarioService.authenticate(username, request.getSenhaAtual());
            log.info("Senha atual confirmada para usuário: {}", username);
            // Atualiza para a nova senha
            usuarioService.atualizarSenha(username, request.getNovaSenha());
            log.info("Senha alterada com sucesso para usuário: {}", username);
            return ResponseEntity.ok("Senha alterada com sucesso!");
        } catch (Exception e) {
            log.error("Erro ao trocar senha para usuário autenticado: ", e);
            return ResponseEntity.status(500).body("Erro ao trocar senha: " + e.getMessage());
        }
    }

    // Endpoint para validar token no primeiro login
    @PostMapping("/primeiro-login")
    public ResponseEntity<String> validarTokenPrimeiroLogin(@RequestParam String username, @RequestParam String token) {
        boolean valido = usuarioService.validarTokenPrimeiroLogin(username, token);
        if (valido) {
            return ResponseEntity.ok("Token válido, primeiro login realizado!");
        } else {
            return ResponseEntity.status(400).body("Token inválido ou já utilizado.");
        }
    }

    // Atualiza usuário por ID (qualquer campo, inclusive ativo/admin)
    @PutMapping("/users/{id}")
    public ResponseEntity<UsuarioDTO> atualizarUsuario(@PathVariable Long id, @RequestBody UsuarioDTO usuarioDTO) {
        Usuario usuarioAtualizado = new Usuario();
        usuarioAtualizado.setUsername(usuarioDTO.getUsername());
        usuarioAtualizado.setEmail(usuarioDTO.getEmail());
        usuarioAtualizado.setTenantId(usuarioDTO.getTenantId());
        usuarioAtualizado.setPassword(usuarioDTO.getPassword());
        usuarioAtualizado.setAtivo(usuarioDTO.isAtivo());
        usuarioAtualizado.setAdmin(usuarioDTO.isAdmin());
        Usuario usuario = usuarioService.atualizarUsuario(id, usuarioAtualizado);
        return ResponseEntity.ok(new UsuarioDTO(usuario.getId(), usuario.getUsername(), usuario.getEmail(), usuario.getTenantId(), usuario.isAtivo(), usuario.isAdmin()));
    }

    // Endpoint restrito: lista todos os usuários, só acessível pelo tenantId master
    @GetMapping("/all-users")
    public ResponseEntity<List<UsuarioDTO>> listarTodosUsuariosRestrito() {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        if (!"06660607625".equals(tenantId)) {
            return ResponseEntity.status(403).build();
        }
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        List<UsuarioDTO> usuariosDTO = new ArrayList<>();
        for (Usuario u : usuarios) {
//            log.info("Usuario do banco: id={}, username={}, ativo={}, admin={}, email={}, tenantId={}, dataCadastro={}, dataPrimeiroLogin={}",
//                u.getId(), u.getUsername(), u.isAtivo(), u.isAdmin(), u.getEmail(), u.getTenantId(), u.getDataCadastro(), u.getDataPrimeiroLogin());
            UsuarioDTO dto = new UsuarioDTO(
                u.getId(),
                u.getUsername(),
                u.getNome(),
                null, // nunca retorne senha
                u.getEmail(),
                u.getTenantId(),
                u.isAtivo(),
                u.isAdmin(),
                u.getToken(),
                u.isPrimeiroLogin(),
                u.getDataCadastro(),
                u.getDataPrimeiroLogin()
            );
            log.info("DTO gerado: {}", dto);
            usuariosDTO.add(dto);
        }
        return ResponseEntity.ok(usuariosDTO);
    }

    // Endpoint para consultar acessos dos usuários
    @GetMapping("/acessos-usuarios")
    public ResponseEntity<List<UsuarioAcessosDTO>> listarAcessosUsuarios() {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        List<UsuarioAcessosDTO> acessos = acessoUsuarioService.listarAcessosPorUsuario(usuarios);
        return ResponseEntity.ok(acessos);
    }

    // Endpoint para logout
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam Long acessoId) {
        log.info("Logout solicitado para o acessoId: {}", acessoId);
        acessoUsuarioService.registrarLogout(acessoId);
        return ResponseEntity.ok(Map.of("message", "Logout registrado com sucesso"));
    }

    // Endpoint para renovar access token via refresh token
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshTokenStr = body.get("refreshToken");
        var refreshOpt = refreshTokenService.findByToken(refreshTokenStr);

        if (refreshOpt.isEmpty() || refreshTokenService.isExpired(refreshOpt.get())) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token inválido ou expirado"));
        }

        // Token válido, vamos rotacioná-lo
        var usuario = refreshOpt.get().getUsuario();

        // 1. Excluir o token antigo
        refreshTokenService.deleteByToken(refreshTokenStr);

        // 2. Gerar um novo access token
        String newAccessToken = jwtUtil.generateToken(usuario.getUsername(), usuario.getTenantId());

        // 3. Gerar um novo refresh token
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(usuario);

        // 4. Retornar ambos os novos tokens
        return ResponseEntity.ok(Map.of(
                "token", newAccessToken,
                "refreshToken", newRefreshToken.getToken()
        ));
    }
}
