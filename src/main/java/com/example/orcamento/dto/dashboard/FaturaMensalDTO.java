package com.example.orcamento.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaturaMensalDTO {
    private BigDecimal valor;
    private boolean faturaLancada;
    private BigDecimal valorTerceiros;
}
