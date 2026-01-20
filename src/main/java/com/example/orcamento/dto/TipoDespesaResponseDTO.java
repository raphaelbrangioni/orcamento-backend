package com.example.orcamento.dto;

import lombok.Data;

@Data
public class TipoDespesaResponseDTO {
    private Long id;
    private String nome;
    private String tenantId;
    private CategoriaDTO categoria;
    private SubcategoriaDTO subcategoria;

    @Data
    public static class CategoriaDTO {
        private Long id;
        private String nome;
    }
    @Data
    public static class SubcategoriaDTO {
        private Long id;
        private String nome;
    }
}
