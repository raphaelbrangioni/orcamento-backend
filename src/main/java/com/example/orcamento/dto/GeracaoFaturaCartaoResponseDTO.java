package com.example.orcamento.dto;

import com.example.orcamento.model.StatusGeracaoFaturaCartao;
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
public class GeracaoFaturaCartaoResponseDTO {
    private Long id;
    private Long cartaoCreditoId;
    private String nomeCartao;
    private Integer ano;
    private Integer mes;
    private BigDecimal valorFatura;
    private BigDecimal valorTerceiros;
    private BigDecimal valorProprio;
    private Long despesaId;
    private String nomeDespesa;
    private LocalDate dataVencimentoDespesa;
    private StatusGeracaoFaturaCartao status;
    private String geradoPor;
    private LocalDateTime geradoEm;
    private String ultimoReprocessamentoPor;
    private LocalDateTime ultimoReprocessamentoEm;
    private String observacao;
    private String ajustadoPor;
    private LocalDateTime ajustadoEm;
}
