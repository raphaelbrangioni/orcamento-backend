package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaCorrenteSaldoDiaResponseDTO {
    private Long id;
    private LocalDate data;
    private BigDecimal saldoAbertura;
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    private BigDecimal saldoFechamento;
    private LocalDateTime calculadoEm;
    private ContaCorrenteDTO contaCorrente;
}
