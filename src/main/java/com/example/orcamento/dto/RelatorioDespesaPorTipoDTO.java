package com.example.orcamento.dto;

import lombok.Data;

@Data
public class RelatorioDespesaPorTipoDTO {
    private Long tipoDespesaId;
    private String tipoDespesaNome;
    private Double totalDespesa;
    private Long quantidadeLancamentos;

    public RelatorioDespesaPorTipoDTO(Long tipoDespesaId, String tipoDespesaNome, Double totalDespesa, Long quantidadeLancamentos) {
        this.tipoDespesaId = tipoDespesaId;
        this.tipoDespesaNome = tipoDespesaNome;
        this.totalDespesa = totalDespesa;
        this.quantidadeLancamentos = quantidadeLancamentos;
    }
}