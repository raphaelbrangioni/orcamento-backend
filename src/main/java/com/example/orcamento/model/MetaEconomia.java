package com.example.orcamento.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
    private Double fracaoCripto; // Novo campo para armazenar a fração de cripto
    
    @Column(nullable = true, length = 10)
    private String simboloCripto; // Símbolo da criptomoeda (BTC, ETH, XRP, etc)

    @ManyToOne
    @JoinColumn(name = "subcategoria_id")
    private SubcategoriaDespesa tipoInvestimento;

    @Column(nullable = false)
    private String tenantId;
}