package com.example.orcamento.dto.conciliacao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferenciaConciliadaDTO {
    private MovimentoOfxDTO movimentoBanco;
    private Long movimentacaoId;
    private String transferenciaId;
}
