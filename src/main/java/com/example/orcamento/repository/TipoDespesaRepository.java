package com.example.orcamento.repository;

import com.example.orcamento.model.TipoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoDespesaRepository extends JpaRepository<TipoDespesa, Long> {
    boolean existsByNome(String nome);
    List<TipoDespesa> findByTenantId(String tenantId);
    Optional<TipoDespesa> findByIdAndTenantId(Long id, String tenantId);
    void deleteByIdAndTenantId(Long id, String tenantId);
    boolean existsByNomeAndTenantId(String nome, String tenantId);
    // Buscar o primeiro TipoDespesa por subcategoria e tenant
    Optional<TipoDespesa> findFirstBySubcategoriaIdAndTenantId(Long subcategoriaId, String tenantId);
}
