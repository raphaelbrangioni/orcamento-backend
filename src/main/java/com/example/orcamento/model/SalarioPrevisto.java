package com.example.orcamento.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de SalarioPrevisto
 */
@Entity
@Data // Gera getters, setters, toString, equals e hashCode
@NoArgsConstructor // Construtor vazio
@AllArgsConstructor // Construtor com todos os campos
public class SalarioPrevisto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int ano;

    private String mes; // Ex.: "JANUARY", "FEBRUARY", etc.

    private Double valorPrevisto;

    // Multi-tenant: identificação do tenant (CPF do usuário)
    private String tenantId;
}
