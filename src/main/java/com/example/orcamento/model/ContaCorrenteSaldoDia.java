package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "conta_corrente_saldo_dia",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_conta_corrente_saldo_dia_tenant_conta_data",
                        columnNames = {"tenant_id", "conta_corrente_id", "data"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaCorrenteSaldoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "saldo_abertura", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoAbertura;

    @Column(name = "total_entradas", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalEntradas;

    @Column(name = "total_saidas", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSaidas;

    @Column(name = "saldo_fechamento", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoFechamento;

    @Column(name = "calculado_em", nullable = false)
    private LocalDateTime calculadoEm;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_corrente_id", nullable = false)
    private ContaCorrente contaCorrente;
}
