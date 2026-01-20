package com.example.orcamento.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TipoDespesaCategoriaResponseDTO {
    private Long id;
    private String nome;
    private String icone;
    private List<TipoDespesaSubcategoriaResponseDTO> subcategorias;
}
