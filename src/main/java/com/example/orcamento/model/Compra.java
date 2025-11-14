// src/main/java/com/example/orcamento/model/Compra.java
package com.example.orcamento.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "compras")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "valor_total", nullable = false)
    private BigDecimal valorTotal;

    @Column(name = "numero_parcelas", nullable = false)
    private Integer numeroParcelas;

    @Column(name = "data_compra", nullable = false)
    private LocalDate dataCompra;

    @ManyToOne
    @JoinColumn(name = "cartao_credito_id", nullable = false)
    private CartaoCredito cartaoCredito;

    @ManyToOne
    @JoinColumn(name = "subcategoria_id", nullable = false)
    private SubcategoriaDespesa subcategoria;

    @Column(nullable = false)
    private String proprietario;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL)
    @JsonIgnore // Ignora o campo parcelas na serialização
    private List<LancamentoCartao> parcelas;

    @Enumerated(EnumType.STRING)
    @Column(name = "classificacao")
    private TipoClassificacaoDespesa classificacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "variabilidade")
    private TipoVariabilidadeDespesa variabilidade;

    @Column(name = "tenant_id", nullable = false, length = 20)
    private String tenantId;
}