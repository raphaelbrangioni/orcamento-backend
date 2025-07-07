package com.example.orcamento.repository;

import com.example.orcamento.model.AcessoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcessoUsuarioRepository extends JpaRepository<AcessoUsuario, Long> {
}
