package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContaCorrenteDTO {
    private Long id;
    private String agencia;
    private String numeroConta;
    private String banco;
    private String nomeBanco;
    private BigDecimal saldo;
}
