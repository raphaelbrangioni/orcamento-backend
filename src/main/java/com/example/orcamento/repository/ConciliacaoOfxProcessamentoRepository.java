package com.example.orcamento.repository;

import com.example.orcamento.model.ConciliacaoOfxProcessamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConciliacaoOfxProcessamentoRepository extends JpaRepository<ConciliacaoOfxProcessamento, Long>, JpaSpecificationExecutor<ConciliacaoOfxProcessamento> {
    Optional<ConciliacaoOfxProcessamento> findTopByTenantIdAndContaCorrenteIdAndHashArquivoOrderByProcessadoEmDesc(
            String tenantId,
            Long contaCorrenteId,
            String hashArquivo
    );
}
