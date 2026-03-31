package com.example.orcamento.dto.dashboard;

import com.example.orcamento.dto.FechamentoMensalResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardHomeDTO {
    private Integer ano;
    private Integer mes;
    private FechamentoMensalResponseDTO fechamentoMensal;
    private DashboardHomeContasDTO contas;
    private BigDecimal totalFaturasCartoes;
    private List<DashboardHomeCartaoDTO> cartoes;
    private List<DashboardHomeAlertaDTO> alertas;
}
