// src/main/java/com/example/orcamento/model/Compra.java
package com.example.orcamento.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "compras")
@Getter
@Setter
@ToString(exclude = {"terceiros", "parcelas"}) // Exclui para evitar referência circular
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

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Evita serialização do grafo de terceiros (quebra o ciclo e evita proxies LAZY)
    private Set<CompraTerceiro> terceiros;
    
    // Implementação manual de equals e hashCode para evitar referência circular
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Compra compra = (Compra) o;
        return Objects.equals(id, compra.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}