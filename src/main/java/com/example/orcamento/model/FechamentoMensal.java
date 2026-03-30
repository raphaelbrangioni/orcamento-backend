package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "fechamento_mensal",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fechamento_mensal_tenant_ano_mes",
                        columnNames = {"tenant_id", "ano", "mes"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FechamentoMensal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "saldo_inicial", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "receitas_realizadas", nullable = false, precision = 19, scale = 2)
    private BigDecimal receitasRealizadas;

    @Column(name = "despesas_do_mes", nullable = false, precision = 19, scale = 2)
    private BigDecimal despesasDoMes;

    @Column(name = "despesas_pagas", nullable = false, precision = 19, scale = 2)
    private BigDecimal despesasPagas;

    @Column(name = "despesas_pagas_no_caixa", nullable = false, precision = 19, scale = 2)
    private BigDecimal despesasPagasNoCaixa;

    @Column(name = "despesas_pagas_cartao", nullable = false, precision = 19, scale = 2)
    private BigDecimal despesasPagasCartao;

    @Column(name = "total_faturas", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFaturas;

    @Column(name = "total_terceiros_faturas", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalTerceirosFaturas;

    @Column(name = "saldo_final", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "calculado_em", nullable = false)
    private LocalDateTime calculadoEm;
}
