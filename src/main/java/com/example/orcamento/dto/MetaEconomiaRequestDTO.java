package com.example.orcamento.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MetaEconomiaRequestDTO {

    private String nome;
    private double valor;
    private LocalDate dataFinal;
    private Long tipoInvestimentoId; // Receberá o ID da subcategoria
    private double valorEconomizado;
    private Double fracaoBitcoin;
    private String simboloCripto; // Símbolo da criptomoeda (BTC, ETH, XRP, etc)
    private double fracaoCripto;

}
