// src/main/java/com/example/orcamento/model/Movimentacao.java
package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoMovimentacao tipo; // "ENTRADA" ou "SAIDA"

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDateTime dataCadastro; // Data e hora da movimentação

    @Column(nullable = false)
    private LocalDate dataRecebimento; // Data de recebimento ou pagamento

    @Column
    private String descricao; // Detalhes ou observação sobre a movimentação

    @ManyToOne
    @JoinColumn(name = "conta_corrente_id", nullable = false)
    private ContaCorrente contaCorrente;

    @ManyToOne
    @JoinColumn(name = "despesa_id", nullable = true)
    private Despesa despesa; // Referência opcional à despesa

    @ManyToOne
    @JoinColumn(name = "receita_id", nullable = true)
    private Receita receita; // Referência opcional à receita
}