package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class UsuarioDTO {

    private Long id;
    private String username;
    private String password;
    private String email;

    public UsuarioDTO(Long id, String username, String email) {
    }
}

