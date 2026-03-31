package com.example.orcamento.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "fechamento_mensal_historico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FechamentoMensalHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "fechamento_mensal_id")
    private Long fechamentoMensalId;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private Integer mes;

    @Column(nullable = false)
    private String evento;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "ocorrido_em", nullable = false)
    private LocalDateTime ocorridoEm;
}
