package com.example.orcamento.repository;

import com.example.orcamento.model.ContaCorrenteSaldoDia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContaCorrenteSaldoDiaRepository extends JpaRepository<ContaCorrenteSaldoDia, Long> {

    Optional<ContaCorrenteSaldoDia> findByContaCorrenteIdAndTenantIdAndData(Long contaCorrenteId, String tenantId, LocalDate data);

    boolean existsByContaCorrenteIdAndTenantIdAndData(Long contaCorrenteId, String tenantId, LocalDate data);

    boolean existsByContaCorrenteIdAndTenantIdAndDataLessThan(Long contaCorrenteId, String tenantId, LocalDate data);

    List<ContaCorrenteSaldoDia> findByContaCorrenteIdAndTenantIdAndDataBetweenOrderByDataDesc(
            Long contaCorrenteId,
            String tenantId,
            LocalDate dataInicio,
            LocalDate dataFim
    );

    Optional<ContaCorrenteSaldoDia> findTopByContaCorrenteIdAndTenantIdAndDataLessThanOrderByDataDesc(
            Long contaCorrenteId,
            String tenantId,
            LocalDate data
    );

    Optional<ContaCorrenteSaldoDia> findTopByContaCorrenteIdAndTenantIdAndDataLessThanEqualOrderByDataDesc(
            Long contaCorrenteId,
            String tenantId,
            LocalDate data
    );
}
