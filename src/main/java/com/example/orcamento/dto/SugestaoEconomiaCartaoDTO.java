package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SugestaoEconomiaCartaoDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private BigDecimal economiaPotencial;
    private String categoria;
    private String cartao;
}