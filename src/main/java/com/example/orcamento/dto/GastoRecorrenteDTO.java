package com.example.orcamento.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class GastoRecorrenteDTO {
    private Long id;
    private String nome;
    private String frequencia;
    private BigDecimal valorMedio;
    private List<BigDecimal> valores;
    private String categoria;
}
