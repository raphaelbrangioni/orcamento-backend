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
public class DashboardHomeAlertaDTO {
    private String tipo;
    private String nivel;
    private String titulo;
    private String mensagem;
    private Integer quantidade;
    private BigDecimal valor;
}
