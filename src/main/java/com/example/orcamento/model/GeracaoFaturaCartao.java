package com.example.orcamento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "geracao_fatura_cartao",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_geracao_fatura_cartao_tenant_cartao_ano_mes",
                        columnNames = {"tenant_id", "cartao_credito_id", "ano", "mes"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeracaoFaturaCartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cartao_credito_id", nullable = false)
    private CartaoCredito cartaoCredito;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(name = "valor_fatura", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorFatura;

    @Column(name = "valor_terceiros", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTerceiros;

    @Column(name = "valor_proprio", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorProprio;

    @Column(name = "despesa_id")
    private Long despesaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusGeracaoFaturaCartao status;

    @Column(name = "gerado_por", nullable = false)
    private String geradoPor;

    @Column(name = "gerado_em", nullable = false)
    private LocalDateTime geradoEm;

    @Column(name = "ultimo_reprocessamento_por")
    private String ultimoReprocessamentoPor;

    @Column(name = "ultimo_reprocessamento_em")
    private LocalDateTime ultimoReprocessamentoEm;

    @Column(name = "observacao", length = 1000)
    private String observacao;

    @Column(name = "ajustado_por")
    private String ajustadoPor;

    @Column(name = "ajustado_em")
    private LocalDateTime ajustadoEm;
}
