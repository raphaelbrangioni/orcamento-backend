package com.example.orcamento.dto;

import lombok.Data;
import java.util.List;

@Data
public class UsuarioAcessosDTO {
    private Long usuarioId;
    private String username;
    private String nome;
    private List<AcessoUsuarioDTO> acessos;
}
