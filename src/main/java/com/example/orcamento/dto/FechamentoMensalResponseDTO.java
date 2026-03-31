package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FechamentoMensalResponseDTO {
    private Long id;
    private Integer ano;
    private Integer mes;
    private Boolean fechado;
    private BigDecimal saldoInicial;
    private BigDecimal receitasRealizadas;
    private BigDecimal despesasDoMes;
    private BigDecimal despesasPagas;
    private BigDecimal despesasPagasNoCaixa;
    private BigDecimal despesasPagasCartao;
    private BigDecimal totalFaturas;
    private BigDecimal totalTerceirosFaturas;
    private BigDecimal totalFaturasProprias;
    private BigDecimal totalFaturasLancadasComoDespesa;
    private BigDecimal totalFaturasNaoLancadas;
    private BigDecimal saldoFinal;
    private LocalDateTime calculadoEm;
    private String fechadoPor;
    private LocalDateTime fechadoEm;
    private String ultimoReprocessamentoPor;
    private LocalDateTime ultimoReprocessamentoEm;
}
