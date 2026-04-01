package com.example.orcamento.repository;

import com.example.orcamento.model.GeracaoFaturaCartao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GeracaoFaturaCartaoRepository extends JpaRepository<GeracaoFaturaCartao, Long> {
    Optional<GeracaoFaturaCartao> findByIdAndTenantId(Long id, String tenantId);

    Optional<GeracaoFaturaCartao> findByTenantIdAndCartaoCreditoIdAndAnoAndMes(
            String tenantId,
            Long cartaoCreditoId,
            Integer ano,
            Integer mes
    );

    List<GeracaoFaturaCartao> findByTenantIdAndAno(String tenantId, Integer ano);

    List<GeracaoFaturaCartao> findByTenantIdAndAnoAndMesOrderByGeradoEmDesc(String tenantId, Integer ano, Integer mes);
}
