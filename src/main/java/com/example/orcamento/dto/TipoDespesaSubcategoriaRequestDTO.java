package com.example.orcamento.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TipoDespesaSubcategoriaRequestDTO {
    @NotBlank
    private String nome;
}
