package com.example.orcamento.repository;

import com.example.orcamento.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long>, JpaSpecificationExecutor<Despesa> {
    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND d.tipo.id = :tipoId")
    List<Despesa> findByTipoId(@Param("tenantId") String tenantId, @Param("tipoId") Long tipoId);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND ((d.dataPagamento IS NOT NULL AND YEAR(d.dataPagamento) = :ano AND MONTH(d.dataPagamento) = :mes) OR (d.dataPagamento IS NULL AND YEAR(d.dataVencimento) = :ano AND MONTH(d.dataVencimento) = :mes))")
    List<Despesa> findByAnoAndMes(@Param("tenantId") String tenantId, @Param("ano") int ano, @Param("mes") int mes);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND d.dataVencimento BETWEEN :inicio AND :fim")
    List<Despesa> findByDataVencimentoBetween(@Param("tenantId") String tenantId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND YEAR(d.dataVencimento) = :ano AND d.tipo.id = :tipoId")
    List<Despesa> findByAnoAndTipoId(@Param("tenantId") String tenantId, @Param("ano") int ano, @Param("tipoId") Long tipoId);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND d.dataVencimento BETWEEN :dataInicio AND :dataFim AND d.dataPagamento IS NULL")
    List<Despesa> findByDataVencimentoBetweenAndDataPagamentoIsNull(
            @Param("tenantId") String tenantId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND ((d.dataVencimento < :dataReferencia OR d.dataVencimento BETWEEN :dataReferencia AND :dataFim) AND d.dataPagamento IS NULL)")
    List<Despesa> findVencidasEProximas(
            @Param("tenantId") String tenantId,
            @Param("dataReferencia") LocalDate dataReferencia,
            @Param("dataFim") LocalDate dataFim);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND d.dataVencimento >= :dataInicio ORDER BY d.dataVencimento DESC")
    List<Despesa> findByDataVencimentoAfterOrderByDataVencimentoDesc(@Param("tenantId") String tenantId, @Param("dataInicio") LocalDate dataInicio);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND d.dataVencimento >= :dataInicial ORDER BY d.dataVencimento DESC")
    List<Despesa> findDespesasParaAnalise(@Param("tenantId") String tenantId, @Param("dataInicial") LocalDate dataInicial);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND YEAR(d.dataVencimento) = :ano")
    List<Despesa> findDespesasByAno(@Param("tenantId") String tenantId, @Param("ano") int ano);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND d.despesaParceladaId = :despesaParceladaId")
    List<Despesa> findByDespesaParceladaId(@Param("tenantId") String tenantId, @Param("despesaParceladaId") Long despesaParceladaId);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND d.metaEconomia.id = :metaEconomiaId")
    List<Despesa> findByMetaEconomiaId(@Param("tenantId") String tenantId, @Param("metaEconomiaId") Long metaEconomiaId);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId")
    List<Despesa> findByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT d FROM Despesa d WHERE d.tenantId = :tenantId AND d.dataVencimento BETWEEN :inicio AND :fim")
    List<Despesa> findByTenantIdAndDataVencimentoBetween(@Param("tenantId") String tenantId, @Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT d.tipo.id, d.tipo.nome, SUM(d.valorPrevisto), SUM(d.valorPago) " +
            "FROM Despesa d " +
            "WHERE d.tenantId = :tenantId " +
            "AND (:dataInicio IS NULL OR d.dataVencimento >= :dataInicio) " +
            "AND (:dataFim IS NULL OR d.dataVencimento <= :dataFim) " +
            "AND (:tipoDespesaId IS NULL OR d.tipo.id = :tipoDespesaId) " +
            "GROUP BY d.tipo.id, d.tipo.nome")
    List<Object[]> findDespesasPorTipo(
            @Param("tenantId") String tenantId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("tipoDespesaId") Long tipoDespesaId);
}
