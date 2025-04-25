// LancamentoCartaoRepository.java
package com.example.orcamento.repository;

import com.example.orcamento.model.LancamentoCartao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LancamentoCartaoRepository extends JpaRepository<LancamentoCartao, Long> {

    List<LancamentoCartao> findByMesAnoFatura(String mesAnoFatura);

    @Query("SELECT COALESCE(t.nome, 'Sem Tipo'), SUM(l.valorTotal) " +
            "FROM LancamentoCartao l " +
            "LEFT JOIN l.tipoDespesa t " +
            "WHERE l.mesAnoFatura = :mesAnoFatura " +
            "AND (:cartaoId IS NULL OR l.cartaoCredito.id = :cartaoId) " +
            "AND (:proprietario IS NULL OR l.proprietario = :proprietario) " +
            "GROUP BY t.nome")
    List<Object[]> findGastosPorTipoDespesa(
            @Param("mesAnoFatura") String mesAnoFatura,
            @Param("cartaoId") Long cartaoId,
            @Param("proprietario") String proprietario);

    @Query("SELECT c.nome, SUM(l.valorTotal) " +
            "FROM LancamentoCartao l " +
            "JOIN l.cartaoCredito c " +
            "WHERE l.mesAnoFatura = :mesAnoFatura " +
            "GROUP BY c.nome")
    List<Object[]> findGastosPorCartaoCredito(@Param("mesAnoFatura") String mesAnoFatura);

    @Query("SELECT l FROM LancamentoCartao l WHERE YEAR(l.dataCompra) = :ano AND MONTH(l.dataCompra) = :mes")
    List<LancamentoCartao> findByAnoAndMes(@Param("ano") int ano, @Param("mes") int mes);

    @Modifying
    @Query("DELETE FROM LancamentoCartao l WHERE l.compra.id = :compraId")
    void deleteByCompraId(Long compraId);





    // Buscar lançamentos após uma data específica
    List<LancamentoCartao> findByDataCompraAfter(LocalDate data);

    // Buscar lançamentos entre duas datas
    List<LancamentoCartao> findByDataCompraBetween(LocalDate dataInicio, LocalDate dataFim);

    // Buscar lançamentos por cartão e período
    List<LancamentoCartao> findByCartaoCreditoIdAndDataCompraBetween(Long cartaoId, LocalDate dataInicio, LocalDate dataFim);

    // Buscar lançamentos por tipo de despesa
    List<LancamentoCartao> findByTipoDespesaId(Long tipoDespesaId);

    // Buscar lançamentos recorrentes (mesma descrição em diferentes meses)
    @Query("SELECT l FROM LancamentoCartao l WHERE l.descricao = :descricao ORDER BY l.dataCompra")
    List<LancamentoCartao> findByDescricaoOrderByDataCompra(@Param("descricao") String descricao);

    // Buscar gastos por categoria para análise de tendências
    @Query("SELECT COALESCE(t.nome, 'Não categorizado') as categoria, " +
            "SUM(l.valorTotal) as total, " +
            "FUNCTION('YEAR', l.dataCompra) as ano, " +
            "FUNCTION('MONTH', l.dataCompra) as mes " +
            "FROM LancamentoCartao l " +
            "LEFT JOIN l.tipoDespesa t " +
            "WHERE l.dataCompra >= :dataInicio " +
            "GROUP BY categoria, ano, mes " +
            "ORDER BY categoria, ano, mes")
    List<Object[]> findGastosPorCategoriaEMes(@Param("dataInicio") LocalDate dataInicio);

    // Buscar total de gastos por cartão para análise
    @Query("SELECT c.nome as cartao, " +
            "SUM(l.valorTotal) as total, " +
            "FUNCTION('YEAR', l.dataCompra) as ano, " +
            "FUNCTION('MONTH', l.dataCompra) as mes " +
            "FROM LancamentoCartao l " +
            "JOIN l.cartaoCredito c " +
            "WHERE l.dataCompra >= :dataInicio " +
            "GROUP BY cartao, ano, mes " +
            "ORDER BY cartao, ano, mes")
    List<Object[]> findGastosPorCartaoEMes(@Param("dataInicio") LocalDate dataInicio);





//    @Query(value = "SELECT COALESCE(SUM(l.valorTotal), 0) FROM LancamentoCartao l WHERE l.cartaoCredito.id = :cartaoId AND YEAR(l.dataCompra) = :ano AND MONTH(l.dataCompra) = :mes")
//    BigDecimal getFaturaDoMes(@Param("cartaoId") Long cartaoId, @Param("ano") int ano, @Param("mes") int mes);

    @Query("SELECT SUM(l.valorTotal) " +
            "FROM LancamentoCartao l " +
            "WHERE l.cartaoCredito.id = :cartaoId " +
            "AND l.mesAnoFatura = :mesAnoFatura")
    BigDecimal getFaturaDoMes(@Param("cartaoId") Long cartaoId,
                              @Param("mesAnoFatura") String mesAnoFatura);


    @Query("SELECT lc FROM LancamentoCartao lc WHERE (:cartaoId IS NULL OR lc.cartaoCredito.id = :cartaoId) AND (:mesAnoFatura IS NULL OR lc.mesAnoFatura = :mesAnoFatura)")
    List<LancamentoCartao> findByCartaoAndMesAno(@Param("cartaoId") Long cartaoId, @Param("mesAnoFatura") String mesAnoFatura);
}