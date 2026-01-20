package com.example.orcamento.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class TipoDespesaCategoriaRequestDTO {
    @NotBlank
    private String nome;
    private String icone;
    private List<TipoDespesaSubcategoriaRequestDTO> subcategorias;
}
