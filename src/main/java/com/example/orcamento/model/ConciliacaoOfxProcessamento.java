package com.example.orcamento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "conciliacao_ofx_processamento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConciliacaoOfxProcessamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "conta_corrente_id", nullable = false)
    private Long contaCorrenteId;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "tamanho_arquivo", nullable = false)
    private Long tamanhoArquivo;

    @Column(name = "hash_arquivo", length = 64)
    private String hashArquivo;

    @Column(name = "banco_id_ofx")
    private String bancoIdOfx;

    @Column(name = "conta_id_ofx")
    private String contaIdOfx;

    @Column(name = "periodo_inicio")
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim")
    private LocalDate periodoFim;

    @Column(name = "tolerancia_dias", nullable = false)
    private Integer toleranciaDias;

    @Column(name = "tolerancia_valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal toleranciaValor;

    @Column(name = "tolerancia_valor_minimo", nullable = false, precision = 19, scale = 2)
    private BigDecimal toleranciaValorMinimo;

    @Column(name = "conciliados_quantidade", nullable = false)
    private Integer conciliadosQuantidade;

    @Column(name = "banco_sem_pagamento_quantidade", nullable = false)
    private Integer bancoSemPagamentoQuantidade;

    @Column(name = "pagamento_sem_banco_quantidade", nullable = false)
    private Integer pagamentoSemBancoQuantidade;

    @Column(name = "ambiguos_quantidade", nullable = false)
    private Integer ambiguosQuantidade;

    @Column(name = "receitas_conciliadas_quantidade", nullable = false)
    private Integer receitasConciliadasQuantidade;

    @Column(name = "transferencias_conciliadas_quantidade", nullable = false)
    private Integer transferenciasConciliadasQuantidade;

    @Column(name = "creditos_origem_cartao_quantidade", nullable = false)
    private Integer creditosOrigemCartaoQuantidade;

    @Column(name = "banco_sem_receita_quantidade", nullable = false)
    private Integer bancoSemReceitaQuantidade;

    @Column(name = "receita_sem_banco_quantidade", nullable = false)
    private Integer receitaSemBancoQuantidade;

    @Column(name = "receitas_ambiguas_quantidade", nullable = false)
    private Integer receitasAmbiguasQuantidade;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "mensagem_erro", length = 1000)
    private String mensagemErro;

    @Column(name = "processado_em", nullable = false)
    private LocalDateTime processadoEm;
}
