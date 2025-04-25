package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrevisaoGastoCartaoDTO {
    private String categoria;
    private String tendencia;
    private BigDecimal valorPrevisto;
    private Double confianca;
    private String cartao;
}