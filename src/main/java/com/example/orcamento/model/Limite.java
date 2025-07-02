package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "limites")
@Data
public class Limite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tipo_despesa_id", nullable = false)
    private TipoDespesa tipoDespesa;

    @Column(nullable = false)
    private Double valor;

    @Column(nullable = false)
    private String tenantId;
}