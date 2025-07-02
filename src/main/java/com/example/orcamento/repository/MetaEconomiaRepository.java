package com.example.orcamento.repository;

import com.example.orcamento.model.MetaEconomia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetaEconomiaRepository extends JpaRepository<MetaEconomia, Long> {
    List<MetaEconomia> findByTenantId(String tenantId);
    Optional<MetaEconomia> findByIdAndTenantId(Long id, String tenantId);
    void deleteByIdAndTenantId(Long id, String tenantId);
}