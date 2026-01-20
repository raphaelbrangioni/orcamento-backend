package com.example.orcamento.dto;

import lombok.Data;

@Data
public class TipoDespesaRequestDTO {
    private String nome;
    private Long categoriaId;
    private Long subcategoriaId;
    private String tenantId;
}
