package com.example.orcamento.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {
    @JsonProperty("dataCompra")
    private String dataCompra;

    @JsonProperty("estabelecimento")
    private String estabelecimento;

    @JsonProperty("parcela")
    private String parcela;

    @JsonProperty("valor")
    private String valor;

    // Construtor
    public Transaction(String dataCompra, String estabelecimento, String parcela, String valor) {
        this.dataCompra = dataCompra;
        this.estabelecimento = estabelecimento;
        this.parcela = parcela;
        this.valor = valor;
    }

    // Getters
    public String getDataCompra() { return dataCompra; }
    public String getEstabelecimento() { return estabelecimento; }
    public String getParcela() { return parcela; }
    public String getValor() { return valor; }

    // Setters (opcional, mas recomendado para serialização/deserialização completa)
    public void setDataCompra(String dataCompra) { this.dataCompra = dataCompra; }
    public void setEstabelecimento(String estabelecimento) { this.estabelecimento = estabelecimento; }
    public void setParcela(String parcela) { this.parcela = parcela; }
    public void setValor(String valor) { this.valor = valor; }

    @Override
    public String toString() {
        return "Data: " + dataCompra + ", Estabelecimento: " + estabelecimento +
                ", Parcela: " + (parcela != null ? parcela : "N/A") + ", Valor: " + valor;
    }
}