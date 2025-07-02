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

    @Query("SELECT c FROM Compra c WHERE c.tenantId = :tenantId AND (:descricao IS NULL OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :descricao, '%'))) AND (:cartaoId IS NULL OR c.cartaoCredito.id = :cartaoId) AND (:tipoDespesaId IS NULL OR c.tipoDespesa.id = :tipoDespesaId)")
    Page<Compra> findByFilters(
            @Param("descricao") String descricao,
            @Param("cartaoId") Long cartaoId,
            @Param("tipoDespesaId") Long tipoDespesaId,
            @Param("tenantId") String tenantId,
            Pageable pageable);

    @Query("SELECT c FROM Compra c WHERE c.tenantId = :tenantId ORDER BY c.id DESC")
    Page<Compra> findUltimasComprasByTenant(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT c FROM Compra c WHERE c.id = :id AND c.tenantId = :tenantId")
    Compra findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId);
}