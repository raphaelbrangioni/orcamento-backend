package com.example.orcamento.dto;

import lombok.Data;

@Data
public class RelatorioDespesaPorTipoDTO {
    private Long subcategoriaId;
    private String subcategoriaNome;
    private Double totalDespesa;
    private Long quantidadeLancamentos;

    public RelatorioDespesaPorTipoDTO(Long subcategoriaId, String subcategoriaNome, Double totalDespesa, Long quantidadeLancamentos) {
        this.subcategoriaId = subcategoriaId;
        this.subcategoriaNome = subcategoriaNome;
        this.totalDespesa = totalDespesa;
        this.quantidadeLancamentos = quantidadeLancamentos;
    }
}