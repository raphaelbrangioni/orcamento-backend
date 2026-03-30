package com.example.orcamento.dto.conciliacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespesaConciliadaDTO {
    private MovimentoOfxDTO movimento;
    private DespesaConciliacaoDTO despesa;
    private Integer diferencaDias;
}
