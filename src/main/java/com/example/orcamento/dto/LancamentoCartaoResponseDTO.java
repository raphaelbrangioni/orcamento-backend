package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LancamentoCartaoResponseDTO {
    private Long id;
    private String descricao;
    private BigDecimal valorTotal;
    private Integer parcelaAtual;
    private Integer totalParcelas;
    private LocalDate dataCompra;
    private String detalhes;
    private String mesAnoFatura;
    private Long cartaoCreditoId;
    private CategoriaDespesaDTO categoria;
    private String proprietario;
    private String tenantId;
    private LocalDate dataRegistro;
    private Boolean pagoPorTerceiro;
    private String classificacao;
    private String variabilidade;
}
