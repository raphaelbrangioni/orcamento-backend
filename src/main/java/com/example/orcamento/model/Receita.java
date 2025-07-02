package com.example.orcamento.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "receitas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate dataRecebimento;

    @Column(nullable = false)
    private String tipo; // Ex.: "Salário", "Vale-Alimentação", "Outros"

    @Column(name = "is_prevista", nullable = false)
    @JsonProperty("isPrevista") // Garante que o JSON "isPrevista" mapeie corretamente
    private boolean isPrevista;

    // Novo campo
    @ManyToOne
    @JoinColumn(name = "conta_corrente_id")
    private ContaCorrente contaCorrente;
}