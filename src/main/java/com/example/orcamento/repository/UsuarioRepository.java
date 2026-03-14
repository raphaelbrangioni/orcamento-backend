package com.example.orcamento.repository;

import com.example.orcamento.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.data.jpa.repository.Query;
 import org.springframework.data.repository.query.Param;

 import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

     @Query("SELECT DISTINCT u.tenantId FROM Usuario u")
     List<String> findDistinctTenantIds();

     @Query("SELECT u FROM Usuario u WHERE u.tenantId = :tenantId AND u.ativo = true")
     List<Usuario> findAtivosByTenantId(@Param("tenantId") String tenantId);
}
