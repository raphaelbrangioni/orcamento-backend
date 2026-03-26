package com.example.orcamento.repository;

import com.example.orcamento.model.FechamentoMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FechamentoMensalRepository extends JpaRepository<FechamentoMensal, Long> {
    Optional<FechamentoMensal> findByTenantIdAndAnoAndMes(String tenantId, Integer ano, Integer mes);
    List<FechamentoMensal> findByTenantIdOrderByAnoDescMesDesc(String tenantId);
    List<FechamentoMensal> findByTenantIdAndAnoOrderByMesDesc(String tenantId, Integer ano);
    void deleteByTenantIdAndAnoAndMes(String tenantId, Integer ano, Integer mes);
}
