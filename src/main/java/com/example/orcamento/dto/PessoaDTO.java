package com.example.orcamento.dto;

import lombok.Data;

@Data
public class PessoaDTO {
    private Long id;
    private String nome;
    private String observacao;
    private boolean ativo;
}
