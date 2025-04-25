package com.example.orcamento.dto;

import com.example.orcamento.model.DespesaParcelada;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DespesaParceladaDTO {
    private Long id;
    private String descricao;
    private BigDecimal valorTotal;
    private Integer numeroParcelas;
    private LocalDate dataInicial;
    private Month mesPrimeiraParcela;
    private Long tipoDespesaId;
    private String proprietario;
    private String detalhes;
    private LocalDate dataCadastro;

    public DespesaParceladaDTO(DespesaParcelada despesaParcelada) {
        this.id = despesaParcelada.getId();
        this.descricao = despesaParcelada.getDescricao();
        this.valorTotal = despesaParcelada.getValorTotal();
        this.numeroParcelas = despesaParcelada.getNumeroParcelas();
        this.dataInicial = despesaParcelada.getDataInicial();
        this.mesPrimeiraParcela = despesaParcelada.getMesPrimeiraParcela();
        this.tipoDespesaId = despesaParcelada.getTipoDespesa() != null ?
                despesaParcelada.getTipoDespesa().getId() : null;
        this.proprietario = despesaParcelada.getProprietario();
        this.detalhes = despesaParcelada.getDetalhes();
        this.dataCadastro = despesaParcelada.getDataCadastro();
    }
}