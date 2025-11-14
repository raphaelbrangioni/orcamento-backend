package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UsuarioDTO {

    private Long id;
    private String username;
    private String nome;
    private String password;
    private String email;
    private String tenantId;
    private boolean ativo;
    private boolean admin;
    private String token;
    private boolean primeiroLogin;
    private LocalDateTime dataCadastro;
    private LocalDateTime dataPrimeiroLogin;

    public UsuarioDTO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.tenantId = null;
    }

    public UsuarioDTO(Long id, String username, String email, String tenantId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.tenantId = tenantId;
    }

    public UsuarioDTO(Long id, String username, String email, String tenantId, boolean ativo, boolean admin) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.tenantId = tenantId;
        this.ativo = ativo;
        this.admin = admin;
    }

    public UsuarioDTO(Long id, String username, String email, String tenantId, boolean ativo, boolean admin, String token, boolean primeiroLogin) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.tenantId = tenantId;
        this.ativo = ativo;
        this.admin = admin;
        this.token = token;
        this.primeiroLogin = primeiroLogin;
    }

    // Removido construtor duplicado e ajustado para sempre incluir o campo nome
    public UsuarioDTO(Long id, String username, String nome, String email, String tenantId, boolean ativo, boolean admin, String token, boolean primeiroLogin, LocalDateTime dataCadastro, LocalDateTime dataPrimeiroLogin) {
        this.id = id;
        this.username = username;
        this.nome = nome;
        this.email = email;
        this.tenantId = tenantId;
        this.ativo = ativo;
        this.admin = admin;
        this.token = token;
        this.primeiroLogin = primeiroLogin;
        this.dataCadastro = dataCadastro;
        this.dataPrimeiroLogin = dataPrimeiroLogin;
    }
}
