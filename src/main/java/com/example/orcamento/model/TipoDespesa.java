package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_despesa")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoDespesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private String tenantId;
}
