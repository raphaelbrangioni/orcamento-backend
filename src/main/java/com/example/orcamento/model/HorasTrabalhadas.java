package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "horas_trabalhadas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorasTrabalhadas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fim;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column
    private String detalhes;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "projeto_id")
    private Long projetoId;

    @Column(name = "valor_hora")
    private BigDecimal valorHora;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

}
