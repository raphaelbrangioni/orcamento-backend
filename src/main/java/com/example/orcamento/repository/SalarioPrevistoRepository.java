package com.example.orcamento.repository;

import com.example.orcamento.model.SalarioPrevisto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalarioPrevistoRepository extends JpaRepository<SalarioPrevisto, Long> {
    List<SalarioPrevisto> findByAno(int ano);
    List<SalarioPrevisto> findByTenantId(String tenantId);
    List<SalarioPrevisto> findByAnoAndTenantId(int ano, String tenantId);
}
