package com.example.orcamento.dto;

import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.enums.FormaDePagamento;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class DespesaDTO {

    private Long id;
    private String nome;
    private BigDecimal valorPrevisto;
    private BigDecimal valorPago;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private Integer parcela;
    private String detalhes;
    private Long subcategoriaId;
    private String subcategoriaNome;
    private FormaDePagamento formaDePagamento;

    public DespesaDTO(Despesa despesa) {
        this.id = despesa.getId();
        this.nome = despesa.getNome();
        this.valorPrevisto = despesa.getValorPrevisto();
        this.valorPago = despesa.getValorPago();
        this.dataVencimento = despesa.getDataVencimento();
        this.dataPagamento = despesa.getDataPagamento();
        this.parcela = despesa.getParcela();
        this.detalhes = despesa.getDetalhes();
        this.formaDePagamento = despesa.getFormaDePagamento();
        if (despesa.getSubcategoria() != null) {
            this.subcategoriaId = despesa.getSubcategoria().getId();
            this.subcategoriaNome = despesa.getSubcategoria().getNome();
        }
    }
}
