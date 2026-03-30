package com.example.orcamento.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
}
