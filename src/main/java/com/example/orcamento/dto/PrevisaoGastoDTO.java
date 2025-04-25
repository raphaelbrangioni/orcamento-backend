package com.example.orcamento.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PrevisaoGastoDTO {
    private String categoria;
    private BigDecimal valorPrevisto;
    private String tendencia;
    private Double confianca;
}
