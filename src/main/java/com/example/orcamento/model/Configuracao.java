package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuracoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_despesa_investimento_id")
    private Long tipoDespesaInvestimentoId;

    // Podemos adicionar mais configurações no futuro conforme necessário
}