package com.example.orcamento.service;

import com.example.orcamento.dto.TransacaoFinanceiraDTO;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.LancamentoCartao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransacaoFinanceiraService {
    private final DespesaService despesaService;
    private final LancamentoCartaoService lancamentoCartaoService;

    public List<TransacaoFinanceiraDTO> filtrarTransacoesDinamico(Map<String, Object> filtros) {
        List<TransacaoFinanceiraDTO> transacoes = new ArrayList<>();
        Object origemObj = filtros.get("origem");
        String origem = origemObj != null ? origemObj.toString() : null;

        if (origem == null || origem.equalsIgnoreCase("DESPESA")) {
            List<Despesa> despesas = despesaService.listarDespesasPorFiltrosDinamicos(filtros);
            transacoes.addAll(despesas.stream()
                    .map(TransacaoFinanceiraDTO::new)
                    .collect(Collectors.toList()));
        }
        if (origem == null || origem.equalsIgnoreCase("CARTAO_CREDITO")) {
            List<LancamentoCartao> lancamentos = lancamentoCartaoService.listarLancamentosPorFiltrosDinamicos(filtros);
            transacoes.addAll(lancamentos.stream()
                    .map(TransacaoFinanceiraDTO::new)
                    .collect(Collectors.toList()));
        }
        return transacoes;
    }
}
