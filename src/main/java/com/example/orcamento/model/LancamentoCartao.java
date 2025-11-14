package com.example.orcamento.model;

import com.example.orcamento.config.json.CustomLocalDateTimeDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamentos_cartao")
@Data
@Builder
@NoArgsConstructor // Necessário para o Jackson
@AllArgsConstructor // Opcional, mas útil
public class LancamentoCartao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "parcela_atual", nullable = false)
    private Integer parcelaAtual;

    @Column(name = "total_parcelas", nullable = false)
    private Integer totalParcelas;

    @Column(nullable = false)
    private LocalDate dataCompra;

    @Column(name = "detalhes")
    private String detalhes;

    @Column(name = "mes_ano_fatura", nullable = false)
    private String mesAnoFatura; // Novo campo "Mês/Ano da Fatura" (ex.: "MARCO/2025")

    @ManyToOne
    @JoinColumn(name = "cartao_credito_id", nullable = false)
    private CartaoCredito cartaoCredito;

    // Novo campo Proprietário
    @Column(nullable = false)
    private String proprietario; // "Próprio" ou "Terceiros"

    // Novo campo Data de Registro
    @Column(name = "data_registro", nullable = false, updatable = false)
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    private LocalDateTime dataRegistro;

    // Multi-tenant: identificação do tenant (CPF do usuário)
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne
    @JoinColumn(name = "compra_id")
    private Compra compra; // Referência à compra original

    // Getters e setters
    @Setter
    @Getter
    @Column(name = "pago_terceiro")
    private Boolean pagoPorTerceiro = false; // Valor padrão é falso

    @PrePersist
    protected void onCreate() {
        this.dataRegistro = LocalDateTime.now();
    }

    @ManyToOne
    @JoinColumn(name = "subcategoria_id")
    private SubcategoriaDespesa subcategoria;

    // Em LancamentoCartao.java
    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao")
    private TipoClassificacaoDespesa classificacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "variabilidade")
    private TipoVariabilidadeDespesa variabilidade;
}