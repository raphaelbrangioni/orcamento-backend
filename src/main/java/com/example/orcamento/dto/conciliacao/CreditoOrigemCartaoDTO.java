package com.example.orcamento.dto.conciliacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditoOrigemCartaoDTO {
    private MovimentoOfxDTO creditoOrigemCartao;
    private MovimentoOfxDTO debitoPixRelacionado;
}
