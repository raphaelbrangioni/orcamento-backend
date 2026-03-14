package com.example.orcamento.repository;

import com.example.orcamento.model.SubcategoriaDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubcategoriaDespesaRepository extends JpaRepository<SubcategoriaDespesa, Long> {
    @Query("SELECT MAX(s.id) FROM SubcategoriaDespesa s WHERE s.categoria.id = :categoriaId")
    Long findMaxIdByCategoriaId(@Param("categoriaId") Long categoriaId);

    Optional<SubcategoriaDespesa> findByIdAndTenantId(Long id, String tenantId);
}
