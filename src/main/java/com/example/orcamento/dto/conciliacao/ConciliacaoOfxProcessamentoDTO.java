package com.example.orcamento.dto.conciliacao;

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
public class ConciliacaoOfxProcessamentoDTO {
    private Long id;
    private String username;
    private Long contaCorrenteId;
    private String nomeArquivo;
    private Long tamanhoArquivo;
    private String bancoIdOfx;
    private String contaIdOfx;
    private LocalDate periodoInicio;
    private LocalDate periodoFim;
    private Integer toleranciaDias;
    private BigDecimal toleranciaValor;
    private BigDecimal toleranciaValorMinimo;
    private Integer conciliadosQuantidade;
    private Integer bancoSemPagamentoQuantidade;
    private Integer pagamentoSemBancoQuantidade;
    private Integer ambiguosQuantidade;
    private Integer receitasConciliadasQuantidade;
    private Integer transferenciasConciliadasQuantidade;
    private Integer creditosOrigemCartaoQuantidade;
    private Integer bancoSemReceitaQuantidade;
    private Integer receitaSemBancoQuantidade;
    private Integer receitasAmbiguasQuantidade;
    private String status;
    private String mensagemErro;
    private LocalDateTime processadoEm;
}
