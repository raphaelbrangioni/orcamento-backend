package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String tenantId;

    @Column(name = "ativo", nullable = false, columnDefinition = "BIT(1)")
    private boolean ativo;

    @Column(nullable = false)
    private boolean admin;

    @Column(length = 100)
    private String token;

    @Column(nullable = false)
    private boolean primeiroLogin = true;

    @Column(name = "data_cadastro", nullable = false)
    private java.time.LocalDateTime dataCadastro;

    @Column(name = "data_primeiro_login")
    private java.time.LocalDateTime dataPrimeiroLogin;

    @Column(length = 120, nullable = false)
    private String nome;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    @ToString.Exclude
    private java.util.List<AcessoUsuario> acessos;

    @PrePersist
    public void prePersist() {
        if (dataCadastro == null) {
            dataCadastro = java.time.LocalDateTime.now();
        }
    }
}
