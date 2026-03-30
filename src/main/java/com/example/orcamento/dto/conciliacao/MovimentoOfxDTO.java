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
public class MovimentoOfxDTO {
    private LocalDate data;
    private BigDecimal valor;
    private String memo;
    private String fitId;
}
