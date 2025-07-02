package com.example.orcamento.repository;

import com.example.orcamento.model.Limite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface LimiteRepository extends JpaRepository<Limite, Long> {
    List<Limite> findTop10ByOrderByIdDesc();
    List<Limite> findByTipoDespesaId(Long tipoDespesaId); // Novo método para filtrar por tipo de despesa
    List<Limite> findByTenantId(String tenantId);
    Optional<Limite> findByIdAndTenantId(Long id, String tenantId);
    void deleteByIdAndTenantId(Long id, String tenantId);
    List<Limite> findTop10ByTenantIdOrderByIdDesc(String tenantId);
    List<Limite> findByTipoDespesaIdAndTenantId(Long tipoDespesaId, String tenantId);
}