package com.example.orcamento.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaturaMensalDTO {
    private BigDecimal valor;
    private boolean faturaLancada;
    private BigDecimal valorTerceiros;
    private Long geracaoFaturaId;
    private String geradoPor;
    private LocalDateTime geradoEm;
    private String ultimoReprocessamentoPor;
    private LocalDateTime ultimoReprocessamentoEm;
}
