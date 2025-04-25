package com.example.orcamento.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class DespesaKey {
    private final String nome;
    private final String categoria;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DespesaKey that = (DespesaKey) o;
        return Objects.equals(nome, that.nome) && Objects.equals(categoria, that.categoria);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nome, categoria);
    }
}
