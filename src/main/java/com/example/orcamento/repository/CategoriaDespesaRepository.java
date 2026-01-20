package com.example.orcamento.repository;

import com.example.orcamento.model.CategoriaDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaDespesaRepository extends JpaRepository<CategoriaDespesa, Long> {
    List<CategoriaDespesa> findByTenantId(String tenantId);
    boolean existsByNomeAndTenantId(String nome, String tenantId);
    
    @Query("SELECT c FROM CategoriaDespesa c LEFT JOIN FETCH c.subcategorias WHERE c.tenantId = :tenantId")
    List<CategoriaDespesa> findByTenantIdWithSubcategorias(@Param("tenantId") String tenantId);
}
