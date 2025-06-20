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
    @Query("SELECT d FROM Despesa d WHERE d.tipo.id = :tipoId")
    List<Despesa> findByTipoId(@Param("tipoId") Long tipoId);

    @Query("SELECT d FROM Despesa d WHERE " +
            "((d.dataPagamento IS NOT NULL AND YEAR(d.dataPagamento) = :ano AND MONTH(d.dataPagamento) = :mes) OR " +
            "(d.dataPagamento IS NULL AND YEAR(d.dataVencimento) = :ano AND MONTH(d.dataVencimento) = :mes))")
    List<Despesa> findByAnoAndMes(@Param("ano") int ano, @Param("mes") int mes);

    List<Despesa> findByDataVencimentoBetween(LocalDate inicio, LocalDate fim);

    @Query("SELECT d FROM Despesa d WHERE YEAR(d.dataVencimento) = :ano AND d.tipo.id = :tipoId")
    List<Despesa> findByAnoAndTipoId(int ano, Long tipoId);

    @Query("SELECT d.tipo.id, d.tipo.nome, SUM(d.valorPrevisto), SUM(d.valorPago) " +
            "FROM Despesa d " +
            "WHERE (:dataInicio IS NULL OR d.dataVencimento >= :dataInicio) " +
            "AND (:dataFim IS NULL OR d.dataVencimento <= :dataFim) " +
            "AND (:tipoDespesaId IS NULL OR d.tipo.id = :tipoDespesaId) " +
            "GROUP BY d.tipo.id, d.tipo.nome")
    List<Object[]> findDespesasPorTipo(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("tipoDespesaId") Long tipoDespesaId);

    @Query("SELECT d FROM Despesa d WHERE d.dataVencimento BETWEEN :dataInicio AND :dataFim AND d.dataPagamento IS NULL")
    List<Despesa> findByDataVencimentoBetweenAndDataPagamentoIsNull(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    @Query("SELECT d FROM Despesa d WHERE " +
            "(d.dataVencimento < :dataReferencia OR " +
            "d.dataVencimento BETWEEN :dataReferencia AND :dataFim) " +
            "AND d.dataPagamento IS NULL")
    List<Despesa> findVencidasEProximas(
            @Param("dataReferencia") LocalDate dataReferencia,
            @Param("dataFim") LocalDate dataFim);

    @Query("SELECT d FROM Despesa d WHERE d.dataVencimento >= :dataInicio ORDER BY d.dataVencimento DESC")
    List<Despesa> findByDataVencimentoAfterOrderByDataVencimentoDesc(@Param("dataInicio") LocalDate dataInicio);


    // Adicione este método ao seu DespesaRepository
    @Query("SELECT d FROM Despesa d WHERE " +
            "d.dataVencimento >= :dataInicial " +
            "ORDER BY d.dataVencimento DESC")
    List<Despesa> findDespesasParaAnalise(@Param("dataInicial") LocalDate dataInicial);

    @Query("SELECT d FROM Despesa d WHERE YEAR(d.dataVencimento) = :ano")
    List<Despesa> findDespesasByAno(@Param("ano") int ano);

    List<Despesa> findByDespesaParceladaId(Long despesaParceladaId);

    // No DespesaRepository.java
    List<Despesa> findByMetaEconomiaId(Long metaEconomiaId);


}
