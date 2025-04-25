package com.example.orcamento.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GastoRecorrenteCartaoDTO {
    private Long id;
    private String nome;
    private String frequencia;
    private BigDecimal valorMedio;
    private String categoria;
    private String cartao;
}