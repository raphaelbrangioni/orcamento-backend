package com.example.orcamento.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaDespesaDTO {
    private Long id;
    private String nome;
    private SubcategoriaDespesaDTO subcategoria;
}
