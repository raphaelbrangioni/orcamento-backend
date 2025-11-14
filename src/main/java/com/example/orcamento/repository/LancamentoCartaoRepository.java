// LancamentoCartaoRepository.java
package com.example.orcamento.repository;

import com.example.orcamento.model.LancamentoCartao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LancamentoCartaoRepository extends JpaRepository<LancamentoCartao, Long> , JpaSpecificationExecutor<LancamentoCartao> {

    List<LancamentoCartao> findByMesAnoFaturaAndTenantId(String mesAnoFatura, String tenantId);

    @Query("SELECT COALESCE(s.nome, 'Sem Subcategoria'), SUM(l.valorTotal) " +
            "FROM LancamentoCartao l " +
            "LEFT JOIN l.subcategoria s " +
            "WHERE l.mesAnoFatura = :mesAnoFatura " +
            "AND l.tenantId = :tenantId " +
            "AND (:cartaoId IS NULL OR l.cartaoCredito.id = :cartaoId) " +
            "AND (:proprietario IS NULL OR l.proprietario = :proprietario) " +
            "GROUP BY s.nome")
    List<Object[]> findGastosPorTipoDespesa(
            @Param("mesAnoFatura") String mesAnoFatura,
            @Param("cartaoId") Long cartaoId,
            @Param("proprietario") String proprietario,
            @Param("tenantId") String tenantId);

    @Query("SELECT c.nome, SUM(l.valorTotal) " +
            "FROM LancamentoCartao l " +
            "JOIN l.cartaoCredito c " +
            "WHERE l.mesAnoFatura = :mesAnoFatura " +
            "AND l.tenantId = :tenantId " +
            "GROUP BY c.nome")
    List<Object[]> findGastosPorCartaoCredito(@Param("mesAnoFatura") String mesAnoFatura, @Param("tenantId") String tenantId);

    @Query("SELECT l FROM LancamentoCartao l WHERE YEAR(l.dataCompra) = :ano AND MONTH(l.dataCompra) = :mes AND l.tenantId = :tenantId")
    List<LancamentoCartao> findByAnoAndMesAndTenantId(@Param("ano") int ano, @Param("mes") int mes, @Param("tenantId") String tenantId);

    List<LancamentoCartao> findByDataCompraAfterAndTenantId(LocalDate data, String tenantId);

    List<LancamentoCartao> findByDataCompraBetweenAndTenantId(LocalDate dataInicio, LocalDate dataFim, String tenantId);

    List<LancamentoCartao> findByCartaoCreditoIdAndDataCompraBetweenAndTenantId(Long cartaoId, LocalDate dataInicio, LocalDate dataFim, String tenantId);

    // REMOVIDO: Método legado baseado em tipoDespesa, não compatível com o novo modelo
    // List<LancamentoCartao> findByTipoDespesaIdAndTenantId(Long tipoDespesaId, String tenantId);

    // Buscar lançamentos recorrentes (mesma descrição em diferentes meses)
    @Query("SELECT l FROM LancamentoCartao l WHERE l.descricao = :descricao AND l.tenantId = :tenantId ORDER BY l.dataCompra")
    List<LancamentoCartao> findByDescricaoOrderByDataCompra(@Param("descricao") String descricao, @Param("tenantId") String tenantId);

    // Buscar gastos por categoria para análise de tendências
    @Query("SELECT COALESCE(s.nome, 'Não categorizado') as categoria, " +
            "SUM(l.valorTotal) as total, " +
            "FUNCTION('YEAR', l.dataCompra) as ano, " +
            "FUNCTION('MONTH', l.dataCompra) as mes " +
            "FROM LancamentoCartao l " +
            "LEFT JOIN l.subcategoria s " +
            "WHERE l.dataCompra >= :dataInicio AND l.tenantId = :tenantId " +
            "GROUP BY s.nome, ano, mes " +
            "ORDER BY categoria, ano, mes")
    List<Object[]> findGastosPorCategoriaEMes(@Param("dataInicio") LocalDate dataInicio, @Param("tenantId") String tenantId);

    // Buscar total de gastos por cartão para análise
    @Query("SELECT c.nome as cartao, " +
            "SUM(l.valorTotal) as total, " +
            "FUNCTION('YEAR', l.dataCompra) as ano, " +
            "FUNCTION('MONTH', l.dataCompra) as mes " +
            "FROM LancamentoCartao l " +
            "JOIN l.cartaoCredito c " +
            "WHERE l.dataCompra >= :dataInicio AND l.tenantId = :tenantId " +
            "GROUP BY cartao, ano, mes " +
            "ORDER BY cartao, ano, mes")
    List<Object[]> findGastosPorCartaoEMes(@Param("dataInicio") LocalDate dataInicio, @Param("tenantId") String tenantId);

    @Query("SELECT SUM(l.valorTotal) " +
            "FROM LancamentoCartao l " +
            "WHERE l.cartaoCredito.id = :cartaoId " +
            "AND l.mesAnoFatura = :mesAnoFatura " +
            "AND l.tenantId = :tenantId")
    BigDecimal getFaturaDoMes(@Param("cartaoId") Long cartaoId,
                              @Param("mesAnoFatura") String mesAnoFatura,
                              @Param("tenantId") String tenantId);

    @Query("SELECT SUM(l.valorTotal) " +
            "FROM LancamentoCartao l " +
            "WHERE l.cartaoCredito.id = :cartaoId " +
            "AND l.mesAnoFatura = :mesAnoFatura " +
            "AND l.tenantId = :tenantId " +
            "AND l.proprietario = 'Terceiros'")
    BigDecimal getFaturaDoMesTerceiros(@Param("cartaoId") Long cartaoId,
                              @Param("mesAnoFatura") String mesAnoFatura,
                              @Param("tenantId") String tenantId);

    @Query("SELECT lc FROM LancamentoCartao lc WHERE (:cartaoId IS NULL OR lc.cartaoCredito.id = :cartaoId) AND (:mesAnoFatura IS NULL OR lc.mesAnoFatura = :mesAnoFatura) AND lc.tenantId = :tenantId")
    List<LancamentoCartao> findByCartaoAndMesAno(@Param("cartaoId") Long cartaoId, @Param("mesAnoFatura") String mesAnoFatura, @Param("tenantId") String tenantId);

    List<LancamentoCartao> findByProprietarioAndTenantId(String proprietario, String tenantId);

    List<LancamentoCartao> findByProprietarioAndMesAnoFaturaAndTenantId(String proprietario, String mesAnoFatura, String tenantId);

    List<LancamentoCartao> findByTenantId(String tenantId);

    @Modifying
    @Query("DELETE FROM LancamentoCartao l WHERE l.compra.id = :compraId AND l.tenantId = :tenantId")
    void deleteByCompraIdAndTenantId(@Param("compraId") Long compraId, @Param("tenantId") String tenantId);
}