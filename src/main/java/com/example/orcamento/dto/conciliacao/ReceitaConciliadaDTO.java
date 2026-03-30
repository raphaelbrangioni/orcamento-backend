package com.example.orcamento.dto.conciliacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceitaConciliadaDTO {
    private MovimentoOfxDTO movimento;
    private ReceitaConciliacaoDTO receita;
    private Integer diferencaDias;
}
