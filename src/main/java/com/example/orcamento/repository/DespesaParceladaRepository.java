package com.example.orcamento.repository;

import com.example.orcamento.model.DespesaParcelada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DespesaParceladaRepository extends JpaRepository<DespesaParcelada, Long> {

    @Query("SELECT dp FROM DespesaParcelada dp WHERE dp.tenantId = :tenantId AND " +
            "(:descricao IS NULL OR LOWER(dp.descricao) LIKE LOWER(CONCAT('%', :descricao, '%'))) AND " +
            "(:tipoDespesaId IS NULL OR dp.tipoDespesa.id = :tipoDespesaId)")
    Page<DespesaParcelada> findByFiltros(
            @Param("descricao") String descricao,
            @Param("tipoDespesaId") Long tipoDespesaId,
            @Param("tenantId") String tenantId,
            Pageable pageable);

    @Query("SELECT dp FROM DespesaParcelada dp WHERE dp.id = :id AND dp.tenantId = :tenantId")
    DespesaParcelada findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") String tenantId);
}
