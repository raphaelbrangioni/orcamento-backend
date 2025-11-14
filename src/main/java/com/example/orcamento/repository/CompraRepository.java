package com.example.orcamento.repository;

import com.example.orcamento.model.Compra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraRepository extends JpaRepository<Compra, Long> {

    @Query("SELECT c FROM Compra c WHERE c.tenantId = :tenantId AND (:subcategoriaId IS NULL OR c.subcategoria.id = :subcategoriaId)")
    Page<Compra> findByTenantIdAndSubcategoriaId(@Param("tenantId") String tenantId, @Param("subcategoriaId") Long subcategoriaId, Pageable pageable);

    Page<Compra> findByTenantId(String tenantId, Pageable pageable);

    @Query("SELECT c FROM Compra c WHERE c.tenantId = :tenantId ORDER BY c.id DESC")
    Page<Compra> findUltimasComprasByTenant(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT c FROM Compra c WHERE c.id = :id AND c.tenantId = :tenantId")
    Compra findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId);
}