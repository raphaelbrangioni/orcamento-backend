package com.example.orcamento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FechamentoMensalHistoricoDTO {
    private Long id;
    private Long fechamentoMensalId;
    private Integer ano;
    private Integer mes;
    private String evento;
    private String username;
    private LocalDateTime ocorridoEm;
}
