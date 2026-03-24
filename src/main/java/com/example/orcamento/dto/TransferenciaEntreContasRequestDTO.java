package com.example.orcamento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransferenciaEntreContasRequestDTO {
    private Long contaOrigemId;
    private Long contaDestinoId;
    private BigDecimal valor;
    private LocalDate data;
    private String descricao;
}
