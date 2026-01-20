package com.example.orcamento.model.enums;

public enum ReceitaTipo {
    SALARIO,
    BONUS,
    FREELANCER,
    COMISSAO,
    ALUGUEL,
    INVESTIMENTOS,
    VENDA,
    VALE_ALIMENTACAO,
    RESTITUICAO,
    PGTO_TERCEIROS,
    OUTROS;

    public static ReceitaTipo from(String value) {
        if (value == null || value.isBlank()) return OUTROS;
        try {
            return ReceitaTipo.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OUTROS; // fallback seguro para dados antigos
        }
    }
}
