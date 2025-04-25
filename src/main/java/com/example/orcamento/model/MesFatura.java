// src/main/java/com/example/orcamento/model/MesFatura.java
package com.example.orcamento.model;

public enum MesFatura {
    JANEIRO("01"),
    FEVEREIRO("02"),
    MARCO("03"),
    ABRIL("04"),
    MAIO("05"),
    JUNHO("06"),
    JULHO("07"),
    AGOSTO("08"),
    SETEMBRO("09"),
    OUTUBRO("10"),
    NOVEMBRO("11"),
    DEZEMBRO("12");

    private final String codigo;

    MesFatura(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static MesFatura fromCodigo(String codigo) {
        for (MesFatura mes : MesFatura.values()) {
            if (mes.getCodigo().equals(codigo)) {
                return mes;
            }
        }
        throw new IllegalArgumentException("Código de mês inválido: " + codigo);
    }
}