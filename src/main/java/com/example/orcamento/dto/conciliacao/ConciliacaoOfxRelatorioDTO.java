package com.example.orcamento.dto.conciliacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConciliacaoOfxRelatorioDTO {
    private Long contaCorrenteId;
    private LocalDate periodoInicio;
    private LocalDate periodoFim;
    private Integer toleranciaDias;
    private BigDecimal toleranciaValor;
    private BigDecimal toleranciaValorMinimo;
    private String bancoIdOfx;
    private String contaIdOfx;
    private List<DespesaConciliadaDTO> conciliados;
    private List<MovimentoOfxDTO> bancoSemPagamento;
    private List<DespesaConciliacaoDTO> pagamentoSemBanco;
    private List<DespesaAmbiguaDTO> ambiguos;
    private List<ReceitaConciliadaDTO> receitasConciliadas;
    private List<TransferenciaConciliadaDTO> transferenciasConciliadas;
    private List<CreditoOrigemCartaoDTO> creditosOrigemCartao;
    private List<MovimentoOfxDTO> bancoSemReceita;
    private List<ReceitaConciliacaoDTO> receitaSemBanco;
    private List<ReceitaAmbiguaDTO> receitasAmbiguas;
    private Boolean arquivoJaProcessado;
    private LocalDateTime ultimoProcessamentoEm;
}
