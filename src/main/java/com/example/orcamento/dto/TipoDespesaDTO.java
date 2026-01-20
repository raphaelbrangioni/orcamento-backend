package com.example.orcamento.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TipoDespesaDTO {
    private Long id;
    private String nome;
    private CategoriaDespesaDTO categoria;
}
