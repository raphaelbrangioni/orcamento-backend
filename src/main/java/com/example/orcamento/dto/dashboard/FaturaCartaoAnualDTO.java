package com.example.orcamento.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class FaturaCartaoAnualDTO {
    private Long cartaoId; // O ID do cartão
    private Map<String, BigDecimal> faturasPorMes; // O Nome do mês e o valor correspondente
}