package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
@Entity
@Table(name = "despesa_parcelada")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DespesaParcelada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;
    private BigDecimal valorTotal;
    private Integer numeroParcelas;
    private LocalDate dataInicial;

    @Enumerated(EnumType.STRING)
    private Month mesPrimeiraParcela;

    @ManyToOne
    @JoinColumn(name = "tipo_despesa_id")
    private TipoDespesa tipoDespesa;

    private String proprietario;
    private String detalhes;
    private LocalDate dataCadastro;

    @PrePersist
    public void prePersist() {
        if (dataCadastro == null) {
            dataCadastro = LocalDate.now();
        }
    }
}