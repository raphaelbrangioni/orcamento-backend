package com.example.orcamento.controller;

import com.example.orcamento.dto.LoginRequest;
import com.example.orcamento.dto.LoginResponse;
import com.example.orcamento.dto.UsuarioDTO;
import com.example.orcamento.model.Usuario;
import com.example.orcamento.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
