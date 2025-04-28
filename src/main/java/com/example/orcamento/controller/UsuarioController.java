package com.example.orcamento.controller;

import com.example.orcamento.dto.LoginRequest;
import com.example.orcamento.dto.LoginResponse;
import com.example.orcamento.dto.UsuarioDTO;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor  // Lombok injeta automaticamente o service
//@CrossOrigin(origins = "http://localhost:8080")
@CrossOrigin(origins = "http://localhost")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // 🔹 Login de usuário
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        System.out.println(loginRequest);
        String token = usuarioService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());
        System.out.println("Token: " + token);
        return ResponseEntity.ok(new LoginResponse("Login bem-sucedido!", token));
    }

    // ✅ Novo endpoint para registrar usuários
    @PostMapping("/register")
    public ResponseEntity<UsuarioDTO> register(@RequestBody UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioService.criarUsuario(
                usuarioDTO.getUsername(),
                usuarioDTO.getPassword(),
                usuarioDTO.getEmail()
        );

        return ResponseEntity.ok(new UsuarioDTO(usuario.getId(), usuario.getUsername(), usuario.getEmail()));
    }

    // Método para listar todos os usuários
    @GetMapping("/users")
    public ResponseEntity<List<UsuarioDTO>> listarUsuarios() {

        UsuarioDTO dto = new UsuarioDTO();
        List<UsuarioDTO> usuariosDTO = new ArrayList<>();

        List<Usuario> usuarios = usuarioService.listarUsuarios();
        for (Usuario usuario : usuarios) {
            System.out.println("Usuário: " + usuario.getUsername());

            dto.setId(usuario.getId());
            dto.setUsername(usuario.getUsername());
            dto.setEmail(usuario.getEmail());

        }
        usuariosDTO.add(dto);


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

        log.info("Usuário encontrado: {}", usuario);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        UsuarioDTO dto = UsuarioDTO.builder()
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .id(usuario.getId())
                .password(usuario.getPassword())
                .build();

        return ResponseEntity.ok(dto);
    }

    // Método para atualizar a senha do usuário
    @PutMapping("/users/username/{username}/password")
    public ResponseEntity<String> atualizarSenha(@PathVariable String username, @RequestBody String novaSenha) {
        usuarioService.atualizarSenha(username, novaSenha);
        return ResponseEntity.ok("Senha atualizada com sucesso!");
    }
}
