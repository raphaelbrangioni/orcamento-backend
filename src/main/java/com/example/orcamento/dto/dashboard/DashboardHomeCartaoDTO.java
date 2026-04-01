package com.example.orcamento.dto.dashboard;

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
public class DashboardHomeCartaoDTO {
    private Long cartaoId;
    private String nome;
    private Integer diaVencimento;
    private BigDecimal valorFatura;
    private BigDecimal valorTerceiros;
    private boolean faturaLancada;
    private Long geracaoFaturaId;
    private String geradoPor;
    private LocalDateTime geradoEm;
    private String ultimoReprocessamentoPor;
    private LocalDateTime ultimoReprocessamentoEm;
}
