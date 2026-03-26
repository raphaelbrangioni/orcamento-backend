package com.example.orcamento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FecharSaldoDiaRequestDTO {
    private LocalDate data;
    private BigDecimal saldoAbertura;
}
