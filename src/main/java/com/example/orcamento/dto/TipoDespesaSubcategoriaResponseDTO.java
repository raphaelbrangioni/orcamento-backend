package com.example.orcamento.dto;

import lombok.Data;

@Data
public class TipoDespesaSubcategoriaResponseDTO {
    private Long id;
    private String nome;
    // Remover referência à categoria para evitar recursividade infinita
    // private CategoriaDespesa categoria;
}
