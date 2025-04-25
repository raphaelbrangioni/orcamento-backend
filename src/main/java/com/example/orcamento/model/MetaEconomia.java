package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "metas_economia")
@Data
public class MetaEconomia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private double valor;
    private LocalDate dataFinal;
    private double valorEconomizado;

    @Column(nullable = true)
    private Double fracaoBitcoin; // Novo campo para armazenar a fração de BTC
    // Getters e setters

    @Enumerated(EnumType.STRING) // Mapeia o enum como texto no banco de dados
    @Column(name = "tipo_investimento", nullable = false)
    private TipoInvestimento tipoInvestimento;
}