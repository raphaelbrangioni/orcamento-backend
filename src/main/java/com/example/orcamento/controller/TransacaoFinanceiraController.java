// src/main/java/com/example/orcamento/controller/TransacaoFinanceiraController.java
package com.example.orcamento.controller;

import com.example.orcamento.dto.TransacaoFinanceiraDTO;
import com.example.orcamento.model.Despesa;
import com.example.orcamento.model.LancamentoCartao;
import com.example.orcamento.service.DespesaService;
import com.example.orcamento.service.LancamentoCartaoService;
import com.example.orcamento.service.TransacaoFinanceiraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/transacoes")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost")
@Slf4j
public class TransacaoFinanceiraController {

    private final DespesaService despesaService;
    private final LancamentoCartaoService lancamentoCartaoService;
    private final TransacaoFinanceiraService transacaoFinanceiraService;

    @GetMapping
    public ResponseEntity<List<TransacaoFinanceiraDTO>> listarTransacoes() {
        log.info("Requisição GET em /api/v1/transacoes, listarTransacoes");

        List<TransacaoFinanceiraDTO> despesas = despesaService.listarDespesas()
                .stream()
                .map(TransacaoFinanceiraDTO::new)
                .collect(Collectors.toList());

        List<TransacaoFinanceiraDTO> lancamentos = lancamentoCartaoService.listarLancamentos()
                .stream()
                .map(TransacaoFinanceiraDTO::new)
                .collect(Collectors.toList());

        despesas.addAll(lancamentos);
        return ResponseEntity.ok(despesas);
    }

    @GetMapping("/filtrar-dinamico")
    public ResponseEntity<List<TransacaoFinanceiraDTO>> listarTransacoesPorFiltrosDinamicos(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String descricao,
            @RequestParam(required = false) BigDecimal valor,
            @RequestParam(required = false) String detalhes,
            @RequestParam(required = false) Long tipoDespesaId,
            @RequestParam(required = false) String classificacao,
            @RequestParam(required = false) String variabilidade,
            @RequestParam(required = false) Integer parcela,
            @RequestParam(required = false) Integer totalParcelas,
            @RequestParam(required = false) String dataInicio, // Novo parâmetro
            @RequestParam(required = false) String dataFim,   // Novo parâmetro
            @RequestParam(required = false) String origem) {
        log.info("Requisição GET em /api/v1/transacoes/filtrar-dinamico com filtros");

        Map<String, Object> filtros = new HashMap<>();
        if (id != null) filtros.put("id", id);
        if (descricao != null) filtros.put("descricao", descricao);
        if (valor != null) filtros.put("valor", valor);
        if (detalhes != null) filtros.put("detalhes", detalhes);
        if (tipoDespesaId != null) filtros.put("tipoDespesaId", tipoDespesaId);
        if (classificacao != null) filtros.put("classificacao", classificacao);
        if (variabilidade != null) filtros.put("variabilidade", variabilidade);
        if (parcela != null) filtros.put("parcela", parcela);
        if (totalParcelas != null) filtros.put("totalParcelas", totalParcelas);
        if (dataInicio != null) filtros.put("dataInicio", dataInicio);
        if (dataFim != null) filtros.put("dataFim", dataFim);
        if (origem != null) filtros.put("origem", origem);

        List<TransacaoFinanceiraDTO> transacoes = transacaoFinanceiraService.filtrarTransacoesDinamico(filtros);
        return ResponseEntity.ok(transacoes);
    }
}