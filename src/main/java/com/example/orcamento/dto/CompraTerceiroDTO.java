package com.example.orcamento.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CompraTerceiroDTO {
    private Long pessoaId;
    private BigDecimal percentual;
}
