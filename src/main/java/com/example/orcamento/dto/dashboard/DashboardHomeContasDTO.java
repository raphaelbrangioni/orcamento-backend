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
public class DashboardHomeContasDTO {
    private Integer quantidadeContasAtivas;
    private BigDecimal saldoTotal;
}
