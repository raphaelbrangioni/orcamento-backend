// src/main/java/com/example/orcamento/dto/TransacaoFinanceiraDTO.java
package com.example.orcamento.dto;

import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.model.TipoClassificacaoDespesa;
import com.example.orcamento.model.TipoVariabilidadeDespesa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransacaoFinanceiraDTO {
    private Long id;
    private String descricao;
    private BigDecimal valor;
    private String detalhes;
    private CategoriaDTO categoria;
    private SubcategoriaDTO subcategoria;
    private TipoClassificacaoDespesa classificacao;
    private TipoVariabilidadeDespesa variabilidade;
    private Integer parcela;
    private Integer totalParcelas;
    private String dataReferencia; // String no formato YYYY-MM-DD
    private String origem;

    // Construtor para LancamentoCartao
    public TransacaoFinanceiraDTO(LancamentoCartao lancamento) {
        this.id = lancamento.getId();
        this.descricao = lancamento.getDescricao();
        this.valor = lancamento.getValorTotal();
        this.detalhes = lancamento.getDetalhes();
        if (lancamento.getSubcategoria() != null) {
            this.subcategoria = new SubcategoriaDTO(lancamento.getSubcategoria().getId(), lancamento.getSubcategoria().getNome());
            if (lancamento.getSubcategoria().getCategoria() != null) {
                this.categoria = new CategoriaDTO(lancamento.getSubcategoria().getCategoria().getId(), lancamento.getSubcategoria().getCategoria().getNome());
            }
        }
        this.classificacao = lancamento.getClassificacao();
        this.variabilidade = lancamento.getVariabilidade();
        this.parcela = lancamento.getParcelaAtual();
        this.totalParcelas = lancamento.getTotalParcelas();
        this.dataReferencia = convertMesAnoFaturaToDate(lancamento.getMesAnoFatura()); // Converte MES/ANO para YYYY-MM-DD
        this.origem = "CARTAO_CREDITO";
    }

    // Construtor para Despesa
    public TransacaoFinanceiraDTO(Despesa despesa) {
        this.id = despesa.getId();
        this.descricao = despesa.getNome();
        this.valor = despesa.getValorPrevisto();
        this.detalhes = despesa.getDetalhes();
        if (despesa.getSubcategoria() != null) {
            this.subcategoria = new SubcategoriaDTO(despesa.getSubcategoria().getId(), despesa.getSubcategoria().getNome());
            if (despesa.getSubcategoria().getCategoria() != null) {
                this.categoria = new CategoriaDTO(despesa.getSubcategoria().getCategoria().getId(), despesa.getSubcategoria().getCategoria().getNome());
            }
        }
        this.classificacao = despesa.getClassificacao();
        this.variabilidade = despesa.getVariabilidade();
        this.parcela = despesa.getParcela();
        this.totalParcelas = null;
        this.dataReferencia = despesa.getDataVencimento() != null
                ? despesa.getDataVencimento().format(DateTimeFormatter.ISO_LOCAL_DATE)
                : null; // Mantém YYYY-MM-DD
        this.origem = "DESPESA";
    }

    // Método auxiliar para converter MES/ANO para YYYY-MM-DD
    private String convertMesAnoFaturaToDate(String mesAnoFatura) {
        if (mesAnoFatura == null) return null;
        String[] partes = mesAnoFatura.split("/");
        if (partes.length != 2) return null; // Retorna null se formato inválido
        String mes = partes[0].toUpperCase();
        String ano = partes[1];
        String mesNumero = switch (mes) {
            case "JANEIRO" -> "01";
            case "FEVEREIRO" -> "02";
            case "MARCO" -> "03";
            case "ABRIL" -> "04";
            case "MAIO" -> "05";
            case "JUNHO" -> "06";
            case "JULHO" -> "07";
            case "AGOSTO" -> "08";
            case "SETEMBRO" -> "09";
            case "OUTUBRO" -> "10";
            case "NOVEMBRO" -> "11";
            case "DEZEMBRO" -> "12";
            default -> throw new IllegalArgumentException("Mês inválido: " + mes);
        };
        return String.format("%s-%s-01", ano, mesNumero); // Ex.: 2025-12-01
    }
}