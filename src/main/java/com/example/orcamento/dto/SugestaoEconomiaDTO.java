package com.example.orcamento.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SugestaoEconomiaDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private BigDecimal economiaPotencial;
    private String categoria;
}
