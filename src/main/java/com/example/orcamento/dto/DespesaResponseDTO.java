package com.example.orcamento.dto;

import com.example.orcamento.model.enums.FormaDePagamento;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DespesaResponseDTO {
    private Long id;
    private String nome;
    private String tenantId;
    private BigDecimal valorPrevisto;
    private BigDecimal valorPago;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private Integer parcela;
    private String detalhes;
    private String classificacao;
    private String variabilidade;
    private ContaCorrenteDTO conta; // Novo campo opcional
    private CategoriaDespesaDTO categoria;
    private FormaDePagamento formaDePagamento;
    private String anexo;
    private MetaEconomiaDTO metaEconomia; // Novo campo
    // Outros campos conforme necessidade
}
