// CartaoCredito.java
package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "cartoes_credito")
@Data
public class CartaoCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome; // Ex.: "Visa", "Mastercard"

    @Column(nullable = false)
    private BigDecimal limite;

    @Column(nullable = false)
    private Integer diaVencimento; // Dia do mês em que a fatura vence

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCartao status;

    @Column(nullable = false)
    private String modeloImportacao = "generico";

    @Column(nullable = false)
    private String tenantId;
}