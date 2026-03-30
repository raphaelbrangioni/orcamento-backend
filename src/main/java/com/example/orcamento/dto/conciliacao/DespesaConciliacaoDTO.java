package com.example.orcamento.dto.conciliacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespesaConciliacaoDTO {
    private Long id;
    private String nome;
    private LocalDate dataPagamento;
    private LocalDate dataVencimento;
    private BigDecimal valorPago;
    private BigDecimal valorPrevisto;
}
