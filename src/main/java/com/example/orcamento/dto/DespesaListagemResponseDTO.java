package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DespesaListagemResponseDTO {

    private List<DespesaResponseDTO> despesas;
    private BigDecimal oMetaEconomia;

}
