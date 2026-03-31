package com.example.orcamento.repository;

import com.example.orcamento.model.FechamentoMensalHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FechamentoMensalHistoricoRepository extends JpaRepository<FechamentoMensalHistorico, Long> {
    List<FechamentoMensalHistorico> findByTenantIdAndAnoAndMesOrderByOcorridoEmDesc(String tenantId, Integer ano, Integer mes);
}
