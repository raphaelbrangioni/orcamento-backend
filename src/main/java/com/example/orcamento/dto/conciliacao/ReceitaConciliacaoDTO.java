package com.example.orcamento.dto.conciliacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceitaConciliacaoDTO {
    private Long id;
    private String descricao;
    private LocalDate dataRecebimento;
    private BigDecimal valor;
}
