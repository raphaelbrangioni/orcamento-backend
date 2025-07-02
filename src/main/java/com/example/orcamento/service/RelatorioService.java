// src/main/java/com/example/orcamento/service/RelatorioService.java
package com.example.orcamento.service;

import com.example.orcamento.repository.DespesaRepository;
import com.example.orcamento.repository.LancamentoCartaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final LancamentoCartaoRepository lancamentoCartaoRepository;
    private final DespesaRepository despesaRepository; // Novo repositório

    public List<Map<String, Object>> getGastosPorTipoDespesa(String mesAnoFatura, Long cartaoId, String proprietario) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Object[]> resultados = lancamentoCartaoRepository.findGastosPorTipoDespesa(mesAnoFatura, cartaoId, proprietario, tenantId);
        return resultados.stream()
                .map(result -> Map.of(
                        "tipoDespesa", result[0] != null ? result[0] : "Sem Tipo",
                        "total", result[1]
                ))
                .collect(Collectors.toList());
    }

    // Adicione ao existente
    public List<Map<String, Object>> getGastosPorCartaoCredito(String mesAnoFatura) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Object[]> resultados = lancamentoCartaoRepository.findGastosPorCartaoCredito(mesAnoFatura, tenantId);
        return resultados.stream()
                .map(result -> Map.of(
                        "cartao", result[0],
                        "total", result[1]
                ))
                .collect(Collectors.toList());
    }

    // Novo método para despesas gerais
    public List<Map<String, Object>> getDespesasPorTipo(LocalDate dataInicio, LocalDate dataFim, Long tipoDespesaId) {
        String tenantId = com.example.orcamento.security.TenantContext.getTenantId();
        List<Object[]> resultados = despesaRepository.findDespesasPorTipo(tenantId, dataInicio, dataFim, tipoDespesaId);
        return resultados.stream()
                .map(result -> Map.of(
                        "tipoDespesaId", result[0] != null ? result[0] : null,
                        "tipoDespesaNome", result[1] != null ? result[1] : "Sem Tipo",
                        "valorPrevisto", result[2] != null ? result[2] : 0,
                        "valorPago", result[3] != null ? result[3] : 0
                ))
                .collect(Collectors.toList());
    }
}